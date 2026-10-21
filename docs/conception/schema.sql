-- =====================================================================
--  KALANSO : serveur de synchronisation (Supabase / PostgreSQL)  -  version 2
--  Ce fichier peut être exécuté plusieurs fois sans danger.
--
--  Après l'avoir exécuté UNE FOIS, lancez dans le SQL Editor :
--      select mk_owner_init('VOTRE-MOT-DE-PASSE-PROPRIETAIRE');
--  (au moins 10 caractères ; c'est le mot de passe du compte « administrateur du site »,
--   le seul compte qui peut créer, modifier ou supprimer des écoles)
-- =====================================================================

create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

-- ---------- Tables (inaccessibles directement : tout passe par les fonctions) ----------
create table if not exists mk_config (
  id int primary key default 1 check (id = 1),
  secret text not null default encode(extensions.gen_random_bytes(32), 'hex'),
  owner_hash text
);
insert into mk_config (id) values (1) on conflict (id) do nothing;

create table if not exists mk_schools (
  id uuid primary key default gen_random_uuid(),
  code text unique not null,
  name text,
  city text,
  rev bigint not null default 0,
  status text not null default 'active' check (status in ('active', 'suspended')),
  expires date,
  last_backup timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists mk_records (
  school_id uuid not null references mk_schools(id) on delete cascade,
  k text not null,
  id text not null,
  y text,
  data jsonb,
  del boolean not null default false,
  rev bigint not null,
  updated_by text,
  updated_at timestamptz not null default now(),
  primary key (school_id, k, id)
);
create index if not exists mk_records_rev_idx on mk_records (school_id, rev);

create table if not exists mk_creds (
  school_id uuid not null references mk_schools(id) on delete cascade,
  login text not null,
  hash text not null,
  kind text not null,
  ref_id text not null,
  role text not null,
  perms text[] not null default '{}',
  cycles text[] not null default '{}',
  must_change boolean not null default false,
  teacher_id text,
  name text,
  off boolean not null default false,
  primary key (school_id, login)
);
create index if not exists mk_creds_ref_idx on mk_creds (school_id, kind, ref_id);

create table if not exists mk_sessions (
  th text primary key,
  school_id uuid not null references mk_schools(id) on delete cascade,
  login text not null,
  kind text not null,
  ref_id text not null,
  role text not null,
  perms text[] not null default '{}',
  cycles text[] not null default '{}',
  must_change boolean not null default false,
  teacher_id text,
  name text,
  expires timestamptz not null,
  created_at timestamptz not null default now()
);

create table if not exists mk_attempts (
  akey text primary key,
  n int not null default 0,
  until timestamptz
);

create table if not exists mk_seq (
  school_id uuid not null references mk_schools(id) on delete cascade,
  name text not null,
  n bigint not null default 0,
  primary key (school_id, name)
);

-- Journal des actions sensibles (paiements, notes après publication, comptes, sauvegardes…)
create table if not exists mk_audit (
  n bigserial primary key,
  school_id uuid not null references mk_schools(id) on delete cascade,
  at timestamptz not null default now(),
  who text,
  action text not null,
  k text,
  rid text,
  info text
);
create index if not exists mk_audit_idx on mk_audit (school_id, n desc);

-- Sauvegarde signée : état de l'export / de la restauration (côté serveur, donc non falsifiable)
create table if not exists mk_xstate (
  school_id uuid primary key references mk_schools(id) on delete cascade,
  kind text not null,
  xid text not null,
  h text not null default '',
  n bigint not null default 0,
  lk text not null default '',
  lid text not null default '',
  next_i int not null default 0,
  rev0 bigint,
  verified boolean not null default false,
  at timestamptz not null default now()
);
create table if not exists mk_stage (
  school_id uuid not null references mk_schools(id) on delete cascade,
  ord bigint not null,
  k text not null,
  id text not null,
  y text,
  data jsonb,
  primary key (school_id, ord)
);

alter table mk_config enable row level security;
alter table mk_schools enable row level security;
alter table mk_records enable row level security;
alter table mk_creds enable row level security;
alter table mk_sessions enable row level security;
alter table mk_attempts enable row level security;
alter table mk_seq enable row level security;
alter table mk_audit enable row level security;
alter table mk_xstate enable row level security;
alter table mk_stage enable row level security;
-- ---------- Fonctions internes (jamais appelables depuis l'extérieur) ----------
create or replace function mki_uid() returns text language sql volatile
set search_path = public, extensions as $$
  select substr(encode(gen_random_bytes(9), 'hex'), 1, 12)
$$;

create or replace function mki_fail(m text) returns jsonb language sql immutable as $$
  select case when m like 'MK:%'
    then jsonb_build_object('code', split_part(m, ':', 2), 'err', split_part(m, ':', 3))
    else jsonb_build_object('code', 'server', 'err', 'Erreur du serveur', 'detail', left(m, 300)) end
$$;

create or replace function mki_merge(a jsonb, b jsonb) returns jsonb language plpgsql immutable as $$
declare res jsonb := coalesce(a, '{}'::jsonb); e record;
begin
  for e in select key, value from jsonb_each(b) loop
    if e.value = 'null'::jsonb then
      res := res - e.key;
    elsif jsonb_typeof(e.value) = 'object' then
      res := jsonb_set(res, array[e.key],
        mki_merge(case when jsonb_typeof(res -> e.key) = 'object' then res -> e.key else '{}'::jsonb end, e.value), true);
    else
      res := jsonb_set(res, array[e.key], e.value, true);
    end if;
  end loop;
  return res;
end $$;

-- Texte canonique d'un JSON (clés triées, nombres normalisés) : sert aux signatures
create or replace function mki_canon(j jsonb) returns text language plpgsql immutable as $$
declare t text := jsonb_typeof(j); r text;
begin
  if j is null or t = 'null' then return 'null'; end if;
  if t = 'object' then
    select '{' || coalesce(string_agg(to_json(key)::text || ':' || mki_canon(value), ',' order by key collate "C"), '') || '}'
      into r from jsonb_each(j);
    return r;
  elsif t = 'array' then
    select '[' || coalesce(string_agg(mki_canon(value), ',' order by ord), '') || ']'
      into r from jsonb_array_elements(j) with ordinality as x(value, ord);
    return r;
  elsif t = 'number' then
    return trim_scale((j #>> '{}')::numeric)::text;
  end if;
  return j::text;
end $$;

create or replace function mki_recdigest(v_k text, v_id text, v_y text, v_data jsonb) returns text
language sql immutable set search_path = public, extensions as $$
  select encode(digest(v_k || '|' || v_id || '|' || coalesce(v_y, '') || '|' || mki_canon(v_data), 'sha256'), 'hex')
$$;

create or replace function mki_can(s mk_sessions, perm text) returns boolean language sql immutable as $$
  select 'all' = any(s.perms) or perm = any(s.perms)
$$;

create or replace function mki_audit(s mk_sessions, act text, v_k text, v_id text, v_info text) returns void
language sql security definer set search_path = public, extensions as $$
  insert into mk_audit (school_id, who, action, k, rid, info) values (s.school_id, s.name, act, v_k, v_id, left(v_info, 200))
$$;

-- Droits de lecture : quels types de données un profil peut recevoir
create or replace function mki_kinds(p text[]) returns text[] language sql immutable as $$
  select array_agg(distinct x) from (
    select unnest(array['school']) as x
    union all select unnest(array['student','class','cs','subject','teacher','opt']) where p && array['students.read','students.write','struct']
    union all select unnest(array['payment','student','class','opt','teacher']) where p && array['pay.read','pay.write']
    union all select unnest(array['exp','teacher','opt']) where p && array['exp.read','exp.write']
    union all select unnest(array['grade','pub','att','class','cs','subject','student','teacher']) where p && array['grades.read']
    union all select unnest(array['att','class','cs','subject','student','teacher']) where p && array['att']
    union all select unnest(array['slot','class','cs','subject','teacher']) where p && array['slot.read','slot.write']
  ) t
$$;

drop function if exists mki_auth(text);
create or replace function mki_auth(tok text, allow_must boolean default false) returns mk_sessions
language plpgsql security definer set search_path = public, extensions as $$
declare s mk_sessions;
begin
  if tok is null or length(tok) < 20 then raise exception 'MK:session:Session expirée, reconnectez-vous'; end if;
  select * into s from mk_sessions where th = encode(digest(tok, 'sha256'), 'hex') and expires > now();
  if not found then raise exception 'MK:session:Session expirée, reconnectez-vous'; end if;
  if exists (select 1 from mk_creds c where c.school_id = s.school_id and c.login = s.login and c.off) then
    raise exception 'MK:session:Compte désactivé';
  end if;
  if s.must_change and not allow_must then raise exception 'MK:mustchange:Changez d''abord votre code PIN'; end if;
  if s.expires < now() + interval '45 days' then
    update mk_sessions set expires = now() + interval '90 days' where th = s.th;
  end if;
  return s;
end $$;

create or replace function mki_cur(sid uuid, kk text, rid text) returns jsonb language sql stable as $$
  select jsonb_build_object('k', r.k, 'id', r.id, 'd', r.data, 'v', r.rev, 'x', r.del)
  from mk_records r where r.school_id = sid and r.k = kk and r.id = rid
$$;

-- L'école peut-elle encore écrire ? (abonnement actif et non expiré)
create or replace function mki_writable(sid uuid) returns boolean language sql stable as $$
  select status = 'active' and (expires is null or expires >= current_date) from mk_schools where id = sid
$$;

create or replace function mki_session(sc mk_schools, c mk_creds) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare tok text := encode(gen_random_bytes(32), 'hex');
begin
  delete from mk_sessions where expires < now();
  insert into mk_sessions (th, school_id, login, kind, ref_id, role, perms, cycles, must_change, teacher_id, name, expires)
  values (encode(digest(tok, 'sha256'), 'hex'), sc.id, c.login, c.kind, c.ref_id, c.role, c.perms, c.cycles, c.must_change, c.teacher_id, c.name, now() + interval '90 days');
  return jsonb_build_object('token', tok, 'role', c.role, 'perms', to_jsonb(c.perms), 'cycles', to_jsonb(c.cycles), 'mustChange', c.must_change, 'login', c.login, 'name', c.name, 'kind', c.kind,
    'refId', c.ref_id, 'teacherId', c.teacher_id, 'school', sc.code, 'writable', mki_writable(sc.id));
end $$;

-- La classe est-elle dans les cycles confiés à ce compte ? (vide = tous les cycles)
create or replace function mki_scope_ok(s mk_sessions, cid text) returns boolean language sql stable as $$
  select coalesce(array_length(s.cycles, 1), 0) = 0
      or exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = cid and r.data->>'cycle' = any(s.cycles))
$$;

create or replace function mki_nextseq(sid uuid, nm text) returns bigint language sql security definer set search_path = public as $$
  insert into mk_seq (school_id, name, n) values (sid, nm, 1)
  on conflict (school_id, name) do update set n = mk_seq.n + 1 returning n
$$;

-- ---------- Écriture d'un enregistrement (droits + validation) ----------
create or replace function mki_amt(v text, d numeric) returns numeric language plpgsql immutable as $$
declare n numeric;
begin
  if v is null or v = '' then return d; end if;
  begin n := v::numeric; exception when others then raise exception 'MK:bad:Montant invalide'; end;
  if n < 0 or n > 1000000000 then raise exception 'MK:bad:Montant invalide'; end if;
  return round(n);
end $$;

create or replace function mki_list(sid uuid, rid text, dflt text[]) returns text[]
language sql stable as $$
  select coalesce((select array(select jsonb_array_elements_text(r.data->'list')) from mk_records r
                   where r.school_id = sid and r.k = 'opt' and r.id = rid and not r.del), dflt)
$$;

create or replace function mki_apply(s mk_sessions, op jsonb, v_rev bigint) returns void
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_k text := op->>'k';
  rid text := op->>'id';
  isdel boolean := coalesce((op->>'del')::boolean, false);
  patch boolean := coalesce((op->>'patch')::boolean, false);
  v_pin text := op->>'pin';
  old mk_records; st mk_records; cl mk_records; pb mk_records;
  nd jsonb; sch jsonb; parts text[]; e record;
  scale numeric; seqn bigint; mat text; v_cls text; v_new jsonb; v_y text;
  nowts text := to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"');
  today text := to_char(now() at time zone 'utc', 'YYYY-MM-DD');
  v_login text; v_role text; v_tid text; v_off boolean; v_perms text[]; v_ok boolean; v_cyc text; v_lst text[]; v_arr jsonb;
  v_adm boolean := (s.role = 'admin');
begin
  if v_k is null or rid is null or rid !~ '^[A-Za-z0-9_:.-]{1,100}$' then raise exception 'MK:bad:Opération invalide'; end if;
  if v_k not in ('school','class','subject','cs','teacher','student','user','grade','att','payment','exp','slot','pub','opt') then
    raise exception 'MK:bad:Type inconnu';
  end if;

  select * into old from mk_records r where r.school_id = s.school_id and r.k = v_k and r.id = rid;
  select r.data into sch from mk_records r where r.school_id = s.school_id and r.k = 'school' and r.id = 'school';
  v_y := case when v_k in ('class','cs','grade','att','payment','exp','slot','pub') then coalesce(old.y, sch->>'year') else null end;

  -- 1) droits
  v_ok := case v_k
    when 'school' then v_adm and not isdel and rid = 'school'
    when 'user' then v_adm
    when 'pub' then v_adm
    when 'class' then mki_can(s, 'struct')
    when 'subject' then mki_can(s, 'struct')
    when 'cs' then mki_can(s, 'struct')
    when 'teacher' then mki_can(s, 'struct')
    when 'student' then mki_can(s, 'students.write')
    when 'grade' then mki_can(s, 'grades.write') or s.role = 'prof'
    when 'att' then mki_can(s, 'att') or s.role = 'prof'
    when 'payment' then mki_can(s, 'pay.write')
    when 'exp' then mki_can(s, 'exp.write')
    when 'slot' then mki_can(s, 'slot.write')
    when 'opt' then v_adm or mki_can(s, 'struct') or (rid = 'motifs' and mki_can(s, 'pay.write')) or (rid = 'expcats' and mki_can(s, 'exp.write'))
    else false end;
  if not v_ok then
    raise exception 'MK:deny:%', case when s.role = 'eleve' then 'Lecture seule' else 'Action non autorisée pour votre profil' end;
  end if;
  if s.role = 'prof' and isdel then raise exception 'MK:deny:Un professeur ne peut pas supprimer'; end if;
  if v_k in ('payment', 'exp') and isdel then raise exception 'MK:deny:Un paiement ou une dépense ne se supprime pas : on l''annule'; end if;
  if v_k in ('pub', 'opt', 'att') and isdel then raise exception 'MK:deny:Suppression impossible'; end if;

  -- 2) suppression (avec contrôle d'intégrité)
  if isdel then
    if old.id is null or old.del then return; end if;
    if (v_k = 'class' and not mki_scope_ok(s, rid)) or (v_k in ('cs', 'student') and not mki_scope_ok(s, old.data->>'classId')) then
      raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés';
    end if;
    if v_k = 'user' then
      if old.id = s.ref_id and s.kind = 'user' then raise exception 'MK:deny:Vous ne pouvez pas supprimer votre propre compte'; end if;
      if old.data->>'role' = 'admin' and not exists (
        select 1 from mk_records r where r.school_id = s.school_id and r.k = 'user' and not r.del and r.id <> rid and r.data->>'role' = 'admin'
      ) then raise exception 'MK:deny:Il doit rester un administrateur'; end if;
      delete from mk_creds where school_id = s.school_id and kind = 'user' and ref_id = rid;
      delete from mk_sessions where school_id = s.school_id and kind = 'user' and ref_id = rid;
      perform mki_audit(s, 'user_delete', v_k, rid, old.data->>'name');
    elsif v_k = 'student' then
      if exists (select 1 from mk_records r where r.school_id = s.school_id and r.k in ('payment', 'grade') and not r.del and r.data->>'sid' = rid) then
        raise exception 'MK:deny:Cet élève a des paiements ou des notes : marquez-le « Sorti » au lieu de le supprimer';
      end if;
      delete from mk_creds where school_id = s.school_id and kind = 'student' and ref_id = rid;
      delete from mk_sessions where school_id = s.school_id and kind = 'student' and ref_id = rid;
      perform mki_audit(s, 'student_delete', v_k, rid, old.data->>'name');
    elsif v_k = 'class' then
      if exists (select 1 from mk_records r where r.school_id = s.school_id and not r.del
                 and ((r.k = 'student' and r.data->>'classId' = rid) or (r.k = 'cs' and r.data->>'classId' = rid))) then
        raise exception 'MK:deny:Cette classe contient encore des élèves ou des matières';
      end if;
    elsif v_k = 'subject' then
      if exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del and r.data->>'subjectId' = rid) then
        raise exception 'MK:deny:Cette matière est encore utilisée dans une classe';
      end if;
    elsif v_k = 'teacher' then
      if exists (select 1 from mk_records r where r.school_id = s.school_id and not r.del
                 and ((r.k in ('cs', 'user', 'class') and r.data->>'teacherId' = rid))) then
        raise exception 'MK:deny:Ce professeur est encore affecté à une classe ou à un compte';
      end if;
    elsif v_k = 'cs' then
      if exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'grade' and not r.del
                 and r.data->>'cls' = old.data->>'classId' and r.data->>'subj' = old.data->>'subjectId') then
        raise exception 'MK:deny:Des notes existent déjà pour cette matière dans cette classe';
      end if;
    end if;
    update mk_records set del = true, rev = v_rev, updated_by = s.name, updated_at = now()
      where school_id = s.school_id and k = v_k and id = rid;
    return;
  end if;

  -- 3) données
  nd := op->'data';
  if nd is null or jsonb_typeof(nd) <> 'object' then raise exception 'MK:bad:Données manquantes'; end if;
  if length(nd::text) > 8000 then raise exception 'MK:bad:Données trop volumineuses'; end if;
  if patch then
    nd := mki_merge(case when old.id is not null and not old.del then old.data else '{}'::jsonb end, nd);
  end if;

  -- 4) validation par type
  if v_k = 'school' then
    if length(trim(coalesce(nd->>'name', ''))) < 2 then raise exception 'MK:bad:Nom de l''école invalide'; end if;
    if coalesce((nd->>'fyStart')::int, 10) not between 1 and 12 then raise exception 'MK:bad:Mois de début invalide'; end if;
    if coalesce((nd->>'nMonths')::int, 9) not between 1 and 12 then raise exception 'MK:bad:Nombre de mois invalide'; end if;
    if coalesce((nd->>'warnAfter')::int, 2) not between 0 and 12 then raise exception 'MK:bad:Seuil d''avertissement invalide'; end if;
    if coalesce((nd->>'compWeight')::numeric, 2) not between 0 and 10 then raise exception 'MK:bad:Poids de la composition invalide'; end if;
    if coalesce((nd->>'fondTrimW')::numeric, 1) not between 0 and 10 then raise exception 'MK:bad:Poids invalide'; end if;
    if nd->>'appStart' is not null and nd->>'appStart' !~ '^\d{4}-(0[1-9]|1[0-2])$' then raise exception 'MK:bad:Mois de début d''utilisation invalide'; end if;
    v_arr := coalesce(nd->'cycles', '["jardin","fond","sec","lycee","pro"]'::jsonb);
    if jsonb_typeof(v_arr) <> 'array' or exists (select 1 from jsonb_array_elements_text(v_arr) x where x not in ('jardin','fond','sec','lycee','pro')) then
      raise exception 'MK:bad:Cycles invalides';
    end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 80), 'code', sch->>'code',
      'city', left(coalesce(nd->>'city', ''), 60), 'phone', left(coalesce(nd->>'phone', ''), 30),
      'address', left(coalesce(nd->>'address', ''), 100), 'academie', left(coalesce(nd->>'academie', ''), 60), 'cap', left(coalesce(nd->>'cap', ''), 60),
      'year', sch->>'year', 'currency', left(coalesce(nullif(nd->>'currency', ''), 'FCFA'), 8),
      'fyStart', coalesce((nd->>'fyStart')::int, 10), 'nMonths', coalesce((nd->>'nMonths')::int, 9),
      'appStart', nd->>'appStart', 'warnAfter', coalesce((nd->>'warnAfter')::int, 2),
      'compWeight', coalesce((nd->>'compWeight')::numeric, 2), 'fondTrimW', coalesce((nd->>'fondTrimW')::numeric, 1), 'cycles', coalesce(sch->'cycles', v_arr),
      'docLang', case when nd->>'docLang' in ('fr','ar','both') then nd->>'docLang' else 'fr' end,
      'lang', case when nd->>'lang' = 'ar' then 'ar' else 'fr' end,
      'nameAr', left(coalesce(nd->>'nameAr', ''), 100), 'cityAr', left(coalesce(nd->>'cityAr', ''), 60), 'headAr', left(coalesce(nd->>'headAr', ''), 100));
    update mk_schools set name = nd->>'name', city = nd->>'city' where id = s.school_id;

  elsif v_k = 'class' then
    v_cyc := nd->>'cycle';
    if v_cyc not in ('jardin','fond','sec','lycee','pro') then raise exception 'MK:bad:Cycle invalide'; end if;
    if sch->'cycles' is not null and not (sch->'cycles') ? v_cyc then raise exception 'MK:bad:Ce cycle n''est pas activé dans votre école'; end if;
    if coalesce(array_length(s.cycles, 1), 0) > 0 and (not (v_cyc = any(s.cycles)) or (old.id is not null and not mki_scope_ok(s, rid))) then
      raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés';
    end if;
    if length(trim(coalesce(nd->>'name', ''))) < 1 then raise exception 'MK:bad:Nom de classe manquant'; end if;
    if coalesce(nd->>'section', '') not in ('', 'petite', 'moyenne', 'grande') then raise exception 'MK:bad:Section invalide'; end if;
    v_tid := nullif(nd->>'teacherId', '');
    if v_tid is not null and not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'teacher' and r.id = v_tid and not r.del) then
      raise exception 'MK:bad:Professeur introuvable';
    end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 40), 'nameAr', left(coalesce(nd->>'nameAr', ''), 40), 'cycle', v_cyc, 'level', left(coalesce(nd->>'level', ''), 30),
      'serie', left(coalesce(nd->>'serie', ''), 30), 'section', coalesce(nd->>'section', ''),
      'exam', coalesce((nd->>'exam')::boolean, false), 'teacherId', v_tid,
      'regFee', mki_amt(nd->>'regFee', 0), 'fee', case when v_cyc = 'pro' then 0 else mki_amt(nd->>'fee', 0) end,
      'ord', coalesce((nd->>'ord')::int, 0));

  elsif v_k = 'subject' then
    if length(trim(coalesce(nd->>'name', ''))) < 2 then raise exception 'MK:bad:Nom de matière invalide'; end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 50), 'nameAr', left(coalesce(nd->>'nameAr', ''), 50));

  elsif v_k = 'teacher' then
    if length(trim(coalesce(nd->>'name', ''))) < 2 then raise exception 'MK:bad:Nom du professeur invalide'; end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 80), 'phone', left(coalesce(nd->>'phone', ''), 30),
      'salary', mki_amt(nd->>'salary', 0));

  elsif v_k = 'cs' then
    parts := string_to_array(rid, ':');
    if array_length(parts, 1) <> 3 or parts[1] <> 'cs' then raise exception 'MK:bad:Identifiant invalide'; end if;
    select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = parts[2] and not r.del;
    if cl.id is null then raise exception 'MK:bad:Classe introuvable'; end if;
    if not mki_scope_ok(s, parts[2]) then raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés'; end if;
    if cl.data->>'cycle' = 'jardin' then raise exception 'MK:bad:Pas de matières notées au jardin d''enfants'; end if;
    if not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'subject' and r.id = parts[3] and not r.del) then raise exception 'MK:bad:Matière introuvable'; end if;
    if coalesce((nd->>'coef')::numeric, 1) not between 0.5 and 20 then raise exception 'MK:bad:Coefficient invalide'; end if;
    v_tid := nullif(nd->>'teacherId', '');
    if v_tid is not null and not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'teacher' and r.id = v_tid and not r.del) then
      raise exception 'MK:bad:Professeur introuvable';
    end if;
    nd := jsonb_build_object('classId', parts[2], 'subjectId', parts[3],
      'coef', case when cl.data->>'cycle' = 'fond' then 1 else coalesce((nd->>'coef')::numeric, 1) end,
      'teacherId', v_tid, 'main', coalesce((nd->>'main')::boolean, false), 'ranked', coalesce((nd->>'ranked')::boolean, true));

  elsif v_k = 'student' then
    if length(trim(coalesce(nd->>'name', ''))) < 2 then raise exception 'MK:bad:Nom de l''élève invalide'; end if;
    if not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = nd->>'classId' and not r.del) then
      raise exception 'MK:bad:Classe introuvable';
    end if;
    perform mki_amt(nd->>'discount', 0);
    if not mki_scope_ok(s, nd->>'classId') or (old.id is not null and not old.del and not mki_scope_ok(s, old.data->>'classId')) then
      raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés';
    end if;
    if old.id is not null and not old.del then
      mat := old.data->>'matricule';
      if (old.data->>'classId') is distinct from (nd->>'classId') then
        perform mki_audit(s, 'student_move', v_k, rid, coalesce(old.data->>'name', '') || ' -> classe ' || (nd->>'classId'));
      end if;
    else
      if v_pin is null or v_pin !~ '^\d{4,8}$' then raise exception 'MK:pin:Code d''accès de l''élève invalide'; end if;
      mat := coalesce(sch->>'code', 'ECO') || '-' || substr(coalesce(sch->>'year', '0000-0000'), 3, 2) || '-' || lpad(mki_nextseq(s.school_id, 'student')::text, 4, '0');
      perform mki_audit(s, 'student_add', v_k, rid, nd->>'name');
    end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 80), 'sex', case when nd->>'sex' = 'F' then 'F' else 'M' end,
      'birth', left(coalesce(nd->>'birth', ''), 10), 'birthPlace', left(coalesce(nd->>'birthPlace', ''), 60), 'classId', nd->>'classId',
      'parent', left(coalesce(nd->>'parent', ''), 80), 'phone', left(coalesce(nd->>'phone', ''), 30),
      'parentPhone', left(coalesce(nd->>'parentPhone', ''), 30), 'parentPhone2', left(coalesce(nd->>'parentPhone2', ''), 30), 'nameAr', left(coalesce(nd->>'nameAr', ''), 80),
      'regDate', left(coalesce(nd->>'regDate', ''), 10), 'discount', mki_amt(nd->>'discount', 0), 'matricule', mat,
      'matriculeAc', left(coalesce(nd->>'matriculeAc', ''), 30), 'left', coalesce((nd->>'left')::boolean, false));

  elsif v_k = 'user' then
    v_login := lower(trim(coalesce(nd->>'login', '')));
    v_role := nd->>'role';
    if v_login !~ '^[a-z0-9._@-]{3,40}$' then raise exception 'MK:bad:Identifiant invalide (3 à 40 caractères, sans espace)'; end if;
    if v_role not in ('admin','direction','comptable','scolarite','censeur','surveillant','prof') then raise exception 'MK:bad:Rôle invalide'; end if;
    if length(trim(coalesce(nd->>'name', ''))) < 2 then raise exception 'MK:bad:Nom invalide'; end if;
    v_tid := nullif(nd->>'teacherId', '');
    if v_role = 'prof' then
      if v_tid is null or not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'teacher' and r.id = v_tid and not r.del) then
        raise exception 'MK:bad:Choisissez le professeur lié à ce compte';
      end if;
      v_perms := array['prof'];
    elsif v_role = 'admin' then
      v_tid := null; v_perms := array['all'];
    else
      v_tid := null;
      if nd->'perms' is null or jsonb_typeof(nd->'perms') <> 'array' then
        v_perms := case v_role
          when 'direction' then array['students.read','grades.read','att','slot.read','slot.write']
          when 'comptable' then array['students.read','pay.read','pay.write','exp.read','exp.write']
          when 'censeur' then array['students.read','students.write','grades.read','att','slot.read','slot.write','struct']
          when 'surveillant' then array['students.read','students.write','att','slot.read','pay.read','pay.write','exp.read','exp.write','struct']
          else array[]::text[] end;
      else
        select coalesce(array_agg(distinct x), '{}') into v_perms from jsonb_array_elements_text(nd->'perms') x;
      end if;
      if exists (select 1 from unnest(v_perms) x where x not in
        ('students.read','students.write','pay.read','pay.write','exp.read','exp.write','grades.read','att','slot.read','slot.write','struct')) then
        raise exception 'MK:bad:Fonction non délégable';
      end if;
      if coalesce(array_length(v_perms, 1), 0) = 0 then raise exception 'MK:bad:Choisissez au moins une fonction pour ce compte'; end if;
    end if;
    v_arr := coalesce(nd->'cycles', '[]'::jsonb);
    if v_role in ('admin', 'prof') then v_arr := '[]'::jsonb; end if;
    if jsonb_typeof(v_arr) <> 'array' or exists (select 1 from jsonb_array_elements_text(v_arr) x
        where x not in ('jardin','fond','sec','lycee','pro') or (sch->'cycles' is not null and not (sch->'cycles') ? x)) then
      raise exception 'MK:bad:Cycles invalides';
    end if;
    if v_role = 'censeur' and jsonb_array_length(v_arr) = 0 and sch->'cycles' ? 'lycee' then v_arr := '["lycee"]'::jsonb; end if;
    select coalesce(array_agg(distinct x), '{}') into v_lst from jsonb_array_elements_text(v_arr) x;
    v_off := coalesce((nd->>'off')::boolean, false);
    if old.id is null and (v_pin is null or v_pin !~ '^\d{4,8}$') then raise exception 'MK:pin:Le code PIN doit avoir 4 à 8 chiffres'; end if;
    if v_pin is not null and v_pin !~ '^\d{4,8}$' then raise exception 'MK:pin:Le code PIN doit avoir 4 à 8 chiffres'; end if;
    if exists (select 1 from mk_creds c where c.school_id = s.school_id and c.login = v_login and not (c.kind = 'user' and c.ref_id = rid)) then
      raise exception 'MK:dup:Cet identifiant existe déjà';
    end if;
    if old.id is not null and old.data->>'role' = 'admin' and (v_role <> 'admin' or v_off) then
      if old.id = s.ref_id and s.kind = 'user' then raise exception 'MK:deny:Vous ne pouvez pas retirer vos propres droits'; end if;
      if not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'user' and not r.del and r.id <> rid
                     and r.data->>'role' = 'admin' and coalesce((r.data->>'off')::boolean, false) = false) then
        raise exception 'MK:deny:Il doit rester un administrateur actif';
      end if;
    end if;
    nd := jsonb_build_object('name', left(trim(nd->>'name'), 80), 'login', v_login, 'role', v_role, 'perms', to_jsonb(v_perms), 'cycles', to_jsonb(v_lst), 'teacherId', v_tid, 'off', v_off);
    perform mki_audit(s, case when old.id is null then 'user_add' else 'user_edit' end, v_k, rid, v_login || ' (' || v_role || ')');

  elsif v_k = 'grade' then
    parts := string_to_array(rid, ':');
    if array_length(parts, 1) <> 4 or parts[1] <> 'g' then raise exception 'MK:bad:Identifiant de note invalide'; end if;
    select * into st from mk_records r where r.school_id = s.school_id and r.k = 'student' and r.id = parts[2] and not r.del;
    if st.id is null then raise exception 'MK:bad:Élève introuvable'; end if;
    v_cls := st.data->>'classId';
    select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = v_cls and not r.del;
    v_cyc := cl.data->>'cycle';
    if not mki_scope_ok(s, v_cls) then raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés'; end if;
    if v_cyc = 'fond' and parts[4] ~ '^M[1-9]$' then
      scale := 10; v_lst := array['C'];
    elsif v_cyc in ('sec','lycee','pro') and parts[4] ~ '^P[1-3]$' then
      scale := 20; v_lst := array['D1','D2','D3','C'];
    elsif parts[4] ~ '^EB[1-3]$' and (v_cyc = 'lycee' or (v_cyc in ('sec','pro') and coalesce((cl.data->>'exam')::boolean, false))) then
      scale := 20; v_lst := array['C'];
    else
      raise exception 'MK:bad:Période non valable pour cette classe';
    end if;
    if not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del
                   and r.data->>'classId' = v_cls and r.data->>'subjectId' = parts[3]
                   and (s.role <> 'prof' or r.data->>'teacherId' = s.teacher_id)) then
      raise exception 'MK:deny:Cette matière n''est pas la vôtre dans cette classe';
    end if;
    v_new := '{}'::jsonb;
    for e in select key, value from jsonb_each(coalesce(nd->'v', '{}'::jsonb)) loop
      if not (e.key = any(v_lst)) then raise exception 'MK:bad:Évaluation inconnue'; end if;
      if jsonb_typeof(e.value) <> 'number' or (e.value #>> '{}')::numeric not between 0 and scale then
        raise exception 'MK:bad:Note invalide (0 à %)', scale;
      end if;
      v_new := v_new || jsonb_build_object(e.key, e.value);
    end loop;
    nd := jsonb_build_object('sid', parts[2], 'subj', parts[3], 'per', parts[4], 'cls', v_cls, 'v', v_new);
    if exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'pub' and r.id = 'pub:' || v_cls || ':' || parts[4]
               and not r.del and r.data->>'on' = 'true') then
      perform mki_audit(s, 'grade_after_pub', v_k, rid, st.data->>'name' || ' ' || parts[4]);
    end if;

  elsif v_k = 'att' then
    parts := string_to_array(rid, ':');
    if array_length(parts, 1) <> 3 or parts[1] <> 'a' or parts[3] !~ '^\d{4}-\d{2}-\d{2}$' then raise exception 'MK:bad:Identifiant d''appel invalide'; end if;
    select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = parts[2] and not r.del;
    if cl.id is null then raise exception 'MK:bad:Classe introuvable'; end if;
    if not mki_scope_ok(s, parts[2]) then raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés'; end if;
    if s.role = 'prof' and not (cl.data->>'teacherId' = s.teacher_id or exists (
         select 1 from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del
           and r.data->>'classId' = parts[2] and r.data->>'teacherId' = s.teacher_id)) then
      raise exception 'MK:deny:Ce n''est pas votre classe';
    end if;
    v_new := '{}'::jsonb;
    for e in select key, value from jsonb_each(coalesce(nd->'marks', '{}'::jsonb)) loop
      if e.value not in ('"A"'::jsonb, '"R"'::jsonb, '"E"'::jsonb) then raise exception 'MK:bad:Statut de présence invalide'; end if;
      if length(e.key) > 24 then raise exception 'MK:bad:Élève invalide'; end if;
      v_new := v_new || jsonb_build_object(e.key, e.value);
    end loop;
    nd := jsonb_build_object('cls', parts[2], 'date', parts[3], 'marks', v_new);

  elsif v_k = 'payment' then
    select * into st from mk_records r where r.school_id = s.school_id and r.k = 'student' and r.id = coalesce(nd->>'sid', old.data->>'sid') and not r.del;
    if st.id is null then raise exception 'MK:bad:Élève introuvable'; end if;
    if not mki_scope_ok(s, st.data->>'classId') then raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés'; end if;
    if old.id is null then
      if jsonb_typeof(nd->'amount') <> 'number' or (nd->>'amount')::numeric <= 0 or (nd->>'amount')::numeric > 1000000000 then
        raise exception 'MK:bad:Montant invalide';
      end if;
      v_lst := array['Inscription', 'Scolarité'] || mki_list(s.school_id, 'motifs', array['Fournitures', 'Examen', 'Autre']);
      if not (coalesce(nd->>'motif', '') = any(v_lst)) then raise exception 'MK:bad:Motif invalide'; end if;
      if coalesce(nd->>'method', '') not in ('Espèces','Orange Money','Moov Money','Wave','Virement','Chèque') then raise exception 'MK:bad:Mode de paiement invalide'; end if;
      select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = st.data->>'classId';
      if nd->>'motif' = 'Scolarité' and cl.data->>'cycle' = 'pro' then raise exception 'MK:bad:Pas de scolarité dans le cycle professionnel'; end if;
      v_arr := coalesce(nd->'ym', '[]'::jsonb);
      if jsonb_typeof(v_arr) <> 'array' or jsonb_array_length(v_arr) > 12
         or exists (select 1 from jsonb_array_elements_text(v_arr) x where x !~ '^\d{4}-\d{2}$') then
        raise exception 'MK:bad:Mois invalides';
      end if;
      seqn := mki_nextseq(s.school_id, 'receipt');
      nd := jsonb_build_object('sid', st.id, 'amount', round((nd->>'amount')::numeric),
        'date', case when nd->>'date' ~ '^\d{4}-\d{2}-\d{2}$' then nd->>'date' else today end,
        'motif', nd->>'motif', 'ym', v_arr, 'method', nd->>'method', 'note', left(coalesce(nd->>'note', ''), 120),
        'receiptNo', 'R' || substr(coalesce(sch->>'year', '0000'), 1, 4) || '-' || lpad(seqn::text, 5, '0'),
        'by', s.name, 'ts', nowts, 'void', false);
      perform mki_audit(s, 'payment_add', v_k, rid, (nd->>'receiptNo') || ' ' || (nd->>'amount') || ' ' || (nd->>'motif'));
    else
      if not (op->'data' ? 'void') then return; end if;
      if not mki_can(s, 'pay.void') then raise exception 'MK:deny:Seul l''administrateur peut annuler un paiement'; end if;
      nd := old.data || jsonb_build_object('void', coalesce((op->'data'->>'void')::boolean, false), 'note', left(coalesce(op->'data'->>'note', old.data->>'note', ''), 120));
      if (nd->>'void')::boolean then
        nd := nd || jsonb_build_object('voidBy', s.name, 'voidAt', nowts);
      else
        nd := nd - 'voidBy' - 'voidAt';
      end if;
      perform mki_audit(s, case when (nd->>'void')::boolean then 'payment_void' else 'payment_unvoid' end, v_k, rid, old.data->>'receiptNo');
    end if;

  elsif v_k = 'exp' then
    if old.id is null then
      if jsonb_typeof(nd->'amount') <> 'number' or (nd->>'amount')::numeric <= 0 or (nd->>'amount')::numeric > 1000000000 then
        raise exception 'MK:bad:Montant invalide';
      end if;
      v_lst := mki_list(s.school_id, 'expcats', array['Salaires', 'Achats', 'Cérémonies', 'Fournitures', 'Autre']);
      if not (coalesce(nd->>'cat', '') = any(v_lst)) then raise exception 'MK:bad:Catégorie de dépense invalide'; end if;
      v_tid := nullif(nd->>'tid', '');
      if v_tid is not null and not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'teacher' and r.id = v_tid and not r.del) then
        raise exception 'MK:bad:Professeur introuvable';
      end if;
      if nd->>'month' is not null and nd->>'month' !~ '^\d{4}-\d{2}$' then raise exception 'MK:bad:Mois invalide'; end if;
      if length(trim(coalesce(nd->>'label', ''))) < 2 and v_tid is null then raise exception 'MK:bad:Précisez le libellé de la dépense'; end if;
      seqn := mki_nextseq(s.school_id, 'exp');
      nd := jsonb_build_object('amount', round((nd->>'amount')::numeric),
        'date', case when nd->>'date' ~ '^\d{4}-\d{2}-\d{2}$' then nd->>'date' else today end,
        'cat', nd->>'cat', 'label', left(coalesce(nd->>'label', ''), 120), 'tid', v_tid, 'month', nd->>'month',
        'method', left(coalesce(nd->>'method', 'Espèces'), 20),
        'no', 'D' || substr(coalesce(sch->>'year', '0000'), 1, 4) || '-' || lpad(seqn::text, 5, '0'),
        'by', s.name, 'ts', nowts, 'void', false);
      perform mki_audit(s, 'exp_add', v_k, rid, (nd->>'no') || ' ' || (nd->>'amount') || ' ' || (nd->>'cat'));
    else
      if not (op->'data' ? 'void') then return; end if;
      if not mki_can(s, 'pay.void') then raise exception 'MK:deny:Seul l''administrateur peut annuler une dépense'; end if;
      nd := old.data || jsonb_build_object('void', coalesce((op->'data'->>'void')::boolean, false));
      if (nd->>'void')::boolean then nd := nd || jsonb_build_object('voidBy', s.name, 'voidAt', nowts); else nd := nd - 'voidBy' - 'voidAt'; end if;
      perform mki_audit(s, case when (nd->>'void')::boolean then 'exp_void' else 'exp_unvoid' end, v_k, rid, old.data->>'no');
    end if;

  elsif v_k = 'opt' then
    if rid not in ('motifs', 'expcats') then raise exception 'MK:bad:Option inconnue'; end if;
    v_arr := nd->'list';
    if v_arr is null or jsonb_typeof(v_arr) <> 'array' or jsonb_array_length(v_arr) > 30 then raise exception 'MK:bad:Liste invalide'; end if;
    select coalesce(array_agg(x order by o), '{}') into v_lst from (
      select left(trim(t.x), 40) as x, min(t.o) as o from jsonb_array_elements_text(v_arr) with ordinality as t(x, o)
      where length(trim(t.x)) > 0 group by left(trim(t.x), 40)) q;
    if rid = 'motifs' then
      v_lst := array(select x from unnest(v_lst) x where x not in ('Inscription', 'Scolarité'));
    end if;
    nd := jsonb_build_object('list', to_jsonb(v_lst));

  elsif v_k = 'pub' then
    parts := string_to_array(rid, ':');
    if array_length(parts, 1) <> 3 or parts[1] <> 'pub' or parts[3] !~ '^(P[1-3]|M[1-9]|EB[1-3])$' then raise exception 'MK:bad:Identifiant de publication invalide'; end if;
    select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'class' and r.id = parts[2] and not r.del;
    if cl.id is null then raise exception 'MK:bad:Classe introuvable'; end if;
    if (cl.data->>'cycle' = 'fond' and parts[3] !~ '^M') or (cl.data->>'cycle' in ('sec','lycee','pro') and parts[3] ~ '^M') or cl.data->>'cycle' = 'jardin' then
      raise exception 'MK:bad:Période non valable pour cette classe';
    end if;
    nd := jsonb_build_object('cls', parts[2], 'per', parts[3], 'on', coalesce((nd->>'on')::boolean, true), 'at', nowts, 'by', s.name);
    perform mki_audit(s, case when (nd->>'on')::boolean then 'publish' else 'unpublish' end, v_k, rid, cl.data->>'name' || ' ' || parts[3]);

  elsif v_k = 'slot' then
    if coalesce(nd->>'day', '') not in ('Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi','Dimanche') then raise exception 'MK:bad:Jour invalide'; end if;
    if coalesce(nd->>'start', '') !~ '^\d{2}:\d{2}$' or coalesce(nd->>'end', '') !~ '^\d{2}:\d{2}$' or nd->>'end' <= nd->>'start' then
      raise exception 'MK:bad:Horaire invalide';
    end if;
    select * into cl from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del
      and r.data->>'classId' = nd->>'classId' and r.data->>'subjectId' = nd->>'subjectId';
    if cl.id is null then raise exception 'MK:bad:Cette matière n''est pas enseignée dans cette classe'; end if;
    if not mki_scope_ok(s, nd->>'classId') then raise exception 'MK:deny:Cette classe n''est pas dans les cycles qui vous sont confiés'; end if;
    v_tid := nullif(cl.data->>'teacherId', '');
    if exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = 'slot' and not r.del and r.id <> rid
               and r.data->>'day' = nd->>'day' and r.data->>'start' < nd->>'end' and nd->>'start' < r.data->>'end'
               and (r.data->>'classId' = nd->>'classId' or (v_tid is not null and r.data->>'teacherId' = v_tid))) then
      raise exception 'MK:conflict:Conflit d''horaire (classe ou professeur déjà occupé)';
    end if;
    nd := jsonb_build_object('classId', nd->>'classId', 'subjectId', nd->>'subjectId', 'teacherId', v_tid, 'day', nd->>'day',
      'start', nd->>'start', 'end', nd->>'end', 'room', left(coalesce(nd->>'room', ''), 20));
  end if;

  -- 5) enregistrement
  insert into mk_records (school_id, k, id, y, data, del, rev, updated_by)
  values (s.school_id, v_k, rid, v_y, nd, false, v_rev, s.name)
  on conflict (school_id, k, id) do update
    set data = excluded.data, del = false, rev = excluded.rev, updated_by = excluded.updated_by, updated_at = now();

  -- 6) comptes de connexion
  if v_k = 'student' then
    if old.id is null or old.del or not exists (select 1 from mk_creds where school_id = s.school_id and kind = 'student' and ref_id = rid) then
      insert into mk_creds (school_id, login, hash, kind, ref_id, role, perms, name, off)
      values (s.school_id, lower(nd->>'matricule'), crypt(coalesce(v_pin, mki_uid()), gen_salt('bf', 8)), 'student', rid, 'eleve', array['eleve'], nd->>'name', (nd->>'left')::boolean)
      on conflict (school_id, login) do update set hash = excluded.hash, name = excluded.name, off = excluded.off, ref_id = excluded.ref_id;
    else
      update mk_creds set name = nd->>'name', off = (nd->>'left')::boolean,
        hash = case when v_pin ~ '^\d{4,8}$' then crypt(v_pin, gen_salt('bf', 8)) else hash end
        where school_id = s.school_id and kind = 'student' and ref_id = rid;
      if v_pin ~ '^\d{4,8}$' or (nd->>'left')::boolean then
        delete from mk_sessions where school_id = s.school_id and kind = 'student' and ref_id = rid;
      end if;
    end if;
  elsif v_k = 'user' then
    if old.id is null or old.del or not exists (select 1 from mk_creds where school_id = s.school_id and kind = 'user' and ref_id = rid) then
      insert into mk_creds (school_id, login, hash, kind, ref_id, role, perms, cycles, must_change, teacher_id, name, off)
      values (s.school_id, nd->>'login', crypt(v_pin, gen_salt('bf', 8)), 'user', rid, nd->>'role', v_perms, v_lst, true, nd->>'teacherId', nd->>'name', (nd->>'off')::boolean);
    else
      update mk_creds set login = nd->>'login', role = nd->>'role', perms = v_perms, cycles = v_lst, teacher_id = nd->>'teacherId', name = nd->>'name', off = (nd->>'off')::boolean,
        must_change = case when v_pin ~ '^\d{4,8}$' and not (rid = s.ref_id and s.kind = 'user') then true else must_change end,
        hash = case when v_pin ~ '^\d{4,8}$' then crypt(v_pin, gen_salt('bf', 8)) else hash end
        where school_id = s.school_id and kind = 'user' and ref_id = rid;
      delete from mk_sessions where school_id = s.school_id and kind = 'user' and ref_id = rid and (rid <> s.ref_id or s.kind <> 'user');
    end if;
  end if;
end $$;
-- ---------- Fonctions publiques (appelées par l'application) ----------
create or replace function mk_ping(p jsonb default '{}'::jsonb) returns jsonb
language sql security definer set search_path = public, extensions as $$
  select jsonb_build_object('ok', true, 'v', 2, 'ready', (select owner_hash is not null from mk_config where id = 1))
$$;

create or replace function mk_login(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  sc mk_schools; c mk_creds; a mk_attempts;
  v_school text := upper(trim(coalesce(p->>'school', '')));
  v_login text := lower(trim(coalesce(p->>'login', '')));
  v_key text;
begin
  v_key := v_school || ':' || v_login;
  select * into a from mk_attempts where akey = v_key;
  if a.until is not null and a.until > now() then
    return jsonb_build_object('code', 'lock', 'err', 'Trop d''essais. Réessayez dans ' || ceil(extract(epoch from a.until - now()) / 60)::int || ' minute(s).');
  end if;
  select * into sc from mk_schools where code = v_school;
  if sc.id is not null then
    select * into c from mk_creds where school_id = sc.id and login = v_login;
  end if;
  if c.login is null or c.hash <> crypt(coalesce(p->>'pin', ''), c.hash) then
    insert into mk_attempts (akey, n, until) values (v_key, 1, null)
      on conflict (akey) do update set n = case when mk_attempts.until is not null and mk_attempts.until <= now() then 1 else mk_attempts.n + 1 end,
        until = case when mk_attempts.n + 1 >= 5 then now() + interval '5 minutes' else null end;
    return jsonb_build_object('code', 'auth', 'err', 'Code école, identifiant ou PIN incorrect');
  end if;
  if c.off then return jsonb_build_object('code', 'off', 'err', 'Ce compte est désactivé'); end if;
  delete from mk_attempts where akey = v_key;
  return mki_session(sc, c);
exception when others then
  return mki_fail(sqlerrm);
end $$;

create or replace function mk_logout(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
begin
  delete from mk_sessions where th = encode(digest(coalesce(p->>'token', ''), 'sha256'), 'hex');
  return jsonb_build_object('ok', true);
end $$;

create or replace function mk_change_pin(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare s mk_sessions; c mk_creds;
begin
  s := mki_auth(p->>'token', true);
  if coalesce(p->>'new', '') !~ '^\d{4,8}$' then return jsonb_build_object('code', 'bad', 'err', 'Le nouveau PIN doit avoir 4 à 8 chiffres'); end if;
  select * into c from mk_creds where school_id = s.school_id and login = s.login;
  if c.login is null or c.hash <> crypt(coalesce(p->>'old', ''), c.hash) then
    return jsonb_build_object('code', 'auth', 'err', 'Ancien PIN incorrect');
  end if;
  update mk_creds set hash = crypt(p->>'new', gen_salt('bf', 8)), must_change = false where school_id = s.school_id and login = s.login;
  update mk_sessions set must_change = false where th = s.th;
  delete from mk_sessions where school_id = s.school_id and login = s.login and th <> s.th;
  return jsonb_build_object('ok', true);
exception when others then
  return mki_fail(sqlerrm);
end $$;

create or replace function mk_pull(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  s mk_sessions; sc mk_schools; since bigint := coalesce((p->>'since')::bigint, 0); lim int := 600;
  v_classes text[] := '{}'; v_pairs text[] := '{}'; v_myclass text; v_kinds text[]; v_year text;
  v_scoped boolean; v_sc_classes text[] := '{}'; v_sc_students text[] := '{}';
  recs jsonb; more boolean; lastrev bigint; top bigint; v_adm boolean;
begin
  s := mki_auth(p->>'token');
  select * into sc from mk_schools where id = s.school_id;
  v_adm := 'all' = any(s.perms);
  select r.data->>'year' into v_year from mk_records r where r.school_id = s.school_id and r.k = 'school' and r.id = 'school';
  if coalesce(p->>'y', '') ~ '^\d{4}-\d{4}$' and v_adm then v_year := p->>'y'; end if;
  v_kinds := mki_kinds(s.perms);
  v_scoped := coalesce(array_length(s.cycles, 1), 0) > 0 and not v_adm and s.role not in ('prof', 'eleve');
  if v_scoped then
    select coalesce(array_agg(r.id), '{}') into v_sc_classes from mk_records r
      where r.school_id = s.school_id and r.k = 'class' and not r.del and r.data->>'cycle' = any(s.cycles);
    select coalesce(array_agg(r.id), '{}') into v_sc_students from mk_records r
      where r.school_id = s.school_id and r.k = 'student' and not r.del and r.data->>'classId' = any(v_sc_classes);
  end if;
  if s.role = 'prof' and s.teacher_id is not null then
    select coalesce(array_agg(distinct x), '{}') into v_classes from (
      select r.data->>'classId' as x from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del and r.data->>'teacherId' = s.teacher_id
      union
      select r.id from mk_records r where r.school_id = s.school_id and r.k = 'class' and not r.del and r.data->>'teacherId' = s.teacher_id) q;
    select coalesce(array_agg(distinct (r.data->>'classId') || '|' || (r.data->>'subjectId')), '{}') into v_pairs
      from mk_records r where r.school_id = s.school_id and r.k = 'cs' and not r.del and r.data->>'teacherId' = s.teacher_id;
  elsif s.role = 'eleve' then
    select r.data->>'classId' into v_myclass from mk_records r where r.school_id = s.school_id and r.k = 'student' and r.id = s.ref_id;
  end if;

  with q as (
    select r.k, r.id, r.rev, r.del,
      case
        when s.role = 'prof' and r.k = 'student' then r.data - 'parent' - 'phone' - 'parentPhone' - 'parentPhone2' - 'birth' - 'discount' - 'regDate'
        when s.role = 'eleve' and r.k = 'att' then
          jsonb_build_object('cls', r.data->'cls', 'date', r.data->'date',
            'marks', case when r.data->'marks' ? s.ref_id then jsonb_build_object(s.ref_id, r.data->'marks'->s.ref_id) else '{}'::jsonb end)
        else r.data end as d,
      row_number() over (order by r.rev) as rn
    from mk_records r
    where r.school_id = s.school_id and r.rev > since
      and (r.y is null or r.y = v_year)
      and case
        when v_adm then true
        when s.role = 'prof' then
          r.k = 'school'
          or (r.k = 'teacher' and r.id = s.teacher_id)
          or (r.k = 'class' and r.id = any(v_classes))
          or (r.k = 'subject' and exists (select 1 from unnest(v_pairs) x where split_part(x, '|', 2) = r.id))
          or (r.k = 'cs' and (r.data->>'classId') || '|' || (r.data->>'subjectId') = any(v_pairs))
          or (r.k = 'student' and r.data->>'classId' = any(v_classes))
          or (r.k = 'grade' and (r.data->>'cls') || '|' || (r.data->>'subj') = any(v_pairs))
          or (r.k = 'pub' and r.data->>'cls' = any(v_classes))
          or (r.k = 'att' and r.data->>'cls' = any(v_classes))
          or (r.k = 'slot' and r.data->>'teacherId' = s.teacher_id)
          or (r.k = 'exp' and r.data->>'tid' = s.teacher_id)
        when s.role = 'eleve' then
          r.k in ('school', 'subject')
          or (r.k = 'class' and r.id = v_myclass)
          or (r.k = 'cs' and r.data->>'classId' = v_myclass)
          or (r.k = 'student' and r.id = s.ref_id)
          or (r.k = 'payment' and r.data->>'sid' = s.ref_id)
          or (r.k = 'pub' and r.data->>'cls' = v_myclass)
          or (r.k = 'grade' and r.data->>'sid' = s.ref_id and exists (
                select 1 from mk_records pb where pb.school_id = s.school_id and pb.k = 'pub'
                  and pb.id = 'pub:' || (r.data->>'cls') || ':' || (r.data->>'per') and not pb.del and pb.data->>'on' = 'true'))
          or (r.k = 'att' and r.data->>'cls' = v_myclass)
          or (r.k = 'slot' and r.data->>'classId' = v_myclass)
        else r.k = any(v_kinds) and r.k <> 'user' and (not v_scoped or case r.k
          when 'class' then r.id = any(v_sc_classes)
          when 'cs' then r.data->>'classId' = any(v_sc_classes)
          when 'student' then r.data->>'classId' = any(v_sc_classes)
          when 'slot' then r.data->>'classId' = any(v_sc_classes)
          when 'grade' then r.data->>'cls' = any(v_sc_classes)
          when 'att' then r.data->>'cls' = any(v_sc_classes)
          when 'pub' then r.data->>'cls' = any(v_sc_classes)
          when 'payment' then r.data->>'sid' = any(v_sc_students)
          else true end) end
    order by r.rev
    limit lim + 1
  )
  select coalesce(jsonb_agg(jsonb_build_object('k', k, 'id', id, 'd', d, 'v', rev, 'x', del) order by rev) filter (where rn <= lim), '[]'::jsonb),
         coalesce(bool_or(rn > lim), false), max(rev) filter (where rn <= lim)
    into recs, more, lastrev from q;
  select rev into top from mk_schools where id = s.school_id;
  return jsonb_build_object('recs', recs, 'more', more, 'rev', coalesce(lastrev, since), 'top', top,
    'meta', jsonb_build_object('status', sc.status, 'expires', sc.expires, 'writable', mki_writable(sc.id), 'year', v_year,
      'lastBackup', case when v_adm then sc.last_backup end, 'now', now()));
exception when others then
  return mki_fail(sqlerrm);
end $$;

create or replace function mk_push(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  s mk_sessions; sc mk_schools; op jsonb; v_rev bigint; res jsonb := '[]'::jsonb; f jsonb; c mk_creds; v_pack boolean := false; v_ok int := 0;
begin
  s := mki_auth(p->>'token');
  -- import d'un paquet reçu par WhatsApp : l'administrateur applique les opérations avec les droits de leur auteur
  if coalesce(p->>'as', '') <> '' then
    if s.role <> 'admin' then return jsonb_build_object('code', 'deny', 'err', 'Réservé à l''administrateur'); end if;
    select * into c from mk_creds where school_id = s.school_id and login = lower(p->>'as') and not off;
    if c.login is null or c.role = 'eleve' then return jsonb_build_object('code', 'bad', 'err', 'Auteur du paquet inconnu : ' || coalesce(p->>'as', '')); end if;
    s.login := c.login; s.role := c.role; s.perms := c.perms; s.cycles := c.cycles; s.teacher_id := c.teacher_id;
    s.name := c.name || ' (paquet)'; s.kind := c.kind; s.ref_id := c.ref_id;
    v_pack := true;
  end if;
  if jsonb_typeof(p->'ops') <> 'array' then return jsonb_build_object('code', 'bad', 'err', 'Requête invalide'); end if;
  if jsonb_array_length(p->'ops') > 150 then return jsonb_build_object('code', 'bad', 'err', 'Trop d''opérations à la fois'); end if;
  select * into sc from mk_schools where id = s.school_id for update;
  if not mki_writable(sc.id) then
    return jsonb_build_object('code', 'readonly', 'err', case when sc.status = 'suspended' then 'Compte suspendu : contactez votre fournisseur' else 'Abonnement expiré : lecture seule' end);
  end if;
  v_rev := sc.rev;
  for op in select value from jsonb_array_elements(p->'ops') loop
    begin
      v_rev := v_rev + 1;
      perform mki_apply(s, op, v_rev);
      v_ok := v_ok + 1;
      res := res || jsonb_build_array(jsonb_build_object('k', op->>'k', 'id', op->>'id', 'ok', true));
    exception when others then
      v_rev := v_rev - 1;
      f := mki_fail(sqlerrm);
      res := res || jsonb_build_array(jsonb_build_object('k', op->>'k', 'id', op->>'id', 'ok', false,
        'err', f->>'err', 'code', f->>'code', 'cur', mki_cur(s.school_id, op->>'k', op->>'id')));
    end;
  end loop;
  update mk_schools set rev = v_rev where id = s.school_id;
  if v_pack then insert into mk_audit (school_id, who, action, info) values (s.school_id, s.name, 'pack_import', v_ok || ' / ' || jsonb_array_length(p->'ops') || ' opérations'); end if;
  return jsonb_build_object('res', res, 'rev', v_rev);
exception when others then
  return mki_fail(sqlerrm);
end $$;

create or replace function mk_audit_list(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare s mk_sessions;
begin
  s := mki_auth(p->>'token');
  if s.role <> 'admin' then return jsonb_build_object('code', 'deny', 'err', 'Réservé à l''administrateur'); end if;
  return jsonb_build_object('rows', coalesce((select jsonb_agg(jsonb_build_object('n', n, 'at', at, 'who', who, 'a', action, 'k', k, 'id', rid, 'i', info) order by n desc)
    from (select * from mk_audit where school_id = s.school_id order by n desc limit 300) t), '[]'::jsonb));
exception when others then
  return mki_fail(sqlerrm);
end $$;
-- ---------- Console « administrateur du site » (un seul compte : le propriétaire) ----------
-- À lancer UNE FOIS dans le SQL Editor (jamais depuis l'application). Relancer = changer le mot de passe.
create or replace function mk_owner_init(pw text) returns text language plpgsql security definer set search_path = public, extensions as $$
begin
  if length(coalesce(pw, '')) < 10 then raise exception 'Mot de passe trop court : 10 caractères minimum'; end if;
  update mk_config set owner_hash = crypt(pw, gen_salt('bf', 10)) where id = 1;
  delete from mk_attempts where akey = 'owner';
  return 'Mot de passe propriétaire enregistré.';
end $$;

create or replace function mk_owner(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  cfg mk_config; a mk_attempts; act text := p->>'act'; sc mk_schools; uid text := mki_uid(); v_adm mk_creds; v_sch jsonb;
  v_code text := upper(trim(coalesce(p->>'code', '')));
  v_login text := lower(trim(coalesce(p->>'login', '')));
  v_pin text := coalesce(p->>'pin', '');
  v_year text := trim(coalesce(p->>'year', ''));
  v_fy int; v_app text; v_cycles jsonb;
begin
  select * into cfg from mk_config where id = 1;
  if cfg.owner_hash is null then return jsonb_build_object('code', 'config', 'err', 'Console non initialisée : lancez select mk_owner_init(''mot de passe'') dans le SQL Editor'); end if;
  select * into a from mk_attempts where akey = 'owner';
  if a.until is not null and a.until > now() then
    return jsonb_build_object('code', 'lock', 'err', 'Trop d''essais. Réessayez dans quelques minutes.');
  end if;
  if cfg.owner_hash <> crypt(coalesce(p->>'pw', ''), cfg.owner_hash) then
    insert into mk_attempts (akey, n, until) values ('owner', 1, null)
      on conflict (akey) do update set n = case when mk_attempts.until is not null and mk_attempts.until <= now() then 1 else mk_attempts.n + 1 end,
        until = case when mk_attempts.n + 1 >= 5 then now() + interval '15 minutes' else null end;
    return jsonb_build_object('code', 'auth', 'err', 'Mot de passe incorrect');
  end if;
  delete from mk_attempts where akey = 'owner';

  if act = 'check' then
    return jsonb_build_object('ok', true);

  elsif act = 'overview' then
    return jsonb_build_object('schools', coalesce((select jsonb_agg(jsonb_build_object('code', s.code, 'name', s.name, 'city', s.city, 'status', s.status,
          'expires', s.expires, 'created', s.created_at, 'lastBackup', s.last_backup,
          'students', (select count(*) from mk_records r where r.school_id = s.id and r.k = 'student' and not r.del),
          'activity', (select max(updated_at) from mk_records r where r.school_id = s.id)) order by s.created_at desc)
          from mk_schools s), '[]'::jsonb));

  elsif act = 'create_school' then
    v_fy := coalesce(nullif(p->>'fyStart', '')::int, 10);
    v_app := nullif(p->>'appStart', '');
    v_cycles := coalesce(p->'cycles', '["jardin","fond","sec","lycee","pro"]'::jsonb);
    if v_code !~ '^[A-Z0-9]{2,8}$' then return jsonb_build_object('code', 'bad', 'err', 'Code école : 2 à 8 lettres ou chiffres'); end if;
    if length(trim(coalesce(p->>'name', ''))) < 2 then return jsonb_build_object('code', 'bad', 'err', 'Nom de l''école invalide'); end if;
    if v_year !~ '^\d{4}-\d{4}$' then return jsonb_build_object('code', 'bad', 'err', 'Année scolaire invalide (ex. 2026-2027)'); end if;
    if v_fy not between 1 and 12 then return jsonb_build_object('code', 'bad', 'err', 'Mois de rentrée invalide'); end if;
    if v_app is not null and v_app !~ '^\d{4}-(0[1-9]|1[0-2])$' then return jsonb_build_object('code', 'bad', 'err', 'Mois de début d''utilisation invalide'); end if;
    if jsonb_typeof(v_cycles) <> 'array' or jsonb_array_length(v_cycles) = 0
       or exists (select 1 from jsonb_array_elements_text(v_cycles) x where x not in ('jardin','fond','sec','lycee','pro')) then
      return jsonb_build_object('code', 'bad', 'err', 'Choisissez au moins un cycle');
    end if;
    if v_login !~ '^[a-z0-9._@-]{3,40}$' then return jsonb_build_object('code', 'bad', 'err', 'Identifiant invalide (3 à 40 caractères, sans espace)'); end if;
    if v_pin !~ '^\d{4,8}$' then return jsonb_build_object('code', 'bad', 'err', 'Le code PIN doit avoir 4 à 8 chiffres'); end if;
    if length(trim(coalesce(p->>'adminName', ''))) < 2 then return jsonb_build_object('code', 'bad', 'err', 'Nom du directeur invalide'); end if;
    if exists (select 1 from mk_schools where code = v_code) then return jsonb_build_object('code', 'dup', 'err', 'Ce code d''école est déjà pris'); end if;
    insert into mk_schools (code, name, city, rev, expires)
      values (v_code, left(trim(p->>'name'), 80), left(coalesce(p->>'city', ''), 60), 4, nullif(p->>'expires', '')::date) returning * into sc;
    v_sch := jsonb_build_object('name', left(trim(p->>'name'), 80), 'code', v_code, 'city', left(coalesce(p->>'city', ''), 60),
      'phone', left(coalesce(p->>'phone', ''), 30), 'address', '', 'academie', '', 'cap', '', 'year', v_year, 'currency', 'FCFA', 'fyStart', v_fy, 'nMonths', 9,
      'appStart', v_app, 'warnAfter', 2, 'compWeight', 2, 'fondTrimW', 1, 'cycles', v_cycles, 'docLang', case when p->>'docLang' in ('fr','ar','both') then p->>'docLang' else 'fr' end,
      'lang', case when p->>'lang' = 'ar' then 'ar' else 'fr' end, 'nameAr', left(coalesce(p->>'nameAr', ''), 100), 'cityAr', '', 'headAr', '');
    insert into mk_records (school_id, k, id, data, rev, updated_by) values
      (sc.id, 'school', 'school', v_sch, 1, 'système'),
      (sc.id, 'user', uid, jsonb_build_object('name', left(trim(p->>'adminName'), 80), 'login', v_login, 'role', 'admin', 'perms', '["all"]'::jsonb,
         'cycles', '[]'::jsonb, 'teacherId', null, 'off', false), 2, 'système'),
      (sc.id, 'opt', 'motifs', jsonb_build_object('list', '["Fournitures","Examen","Autre"]'::jsonb), 3, 'système'),
      (sc.id, 'opt', 'expcats', jsonb_build_object('list', '["Salaires","Achats","Cérémonies","Fournitures","Autre"]'::jsonb), 4, 'système');
    insert into mk_creds (school_id, login, hash, kind, ref_id, role, perms, must_change, name)
      values (sc.id, v_login, crypt(v_pin, gen_salt('bf', 8)), 'user', uid, 'admin', array['all'], true, left(trim(p->>'adminName'), 80));
    insert into mk_audit (school_id, who, action, info) values (sc.id, 'propriétaire', 'school_created', v_code);
    return jsonb_build_object('ok', true, 'code', v_code, 'login', v_login);

  elsif act = 'school_set' then
    select * into sc from mk_schools where code = v_code;
    if sc.id is null then return jsonb_build_object('code', 'bad', 'err', 'École introuvable'); end if;
    if p ? 'status' then
      if p->>'status' not in ('active', 'suspended') then return jsonb_build_object('code', 'bad', 'err', 'Statut invalide'); end if;
      update mk_schools set status = p->>'status' where id = sc.id;
    end if;
    if p ? 'expires' then
      update mk_schools set expires = nullif(p->>'expires', '')::date where id = sc.id;
    end if;
    if p ? 'name' then
      if length(trim(coalesce(p->>'name', ''))) < 2 then return jsonb_build_object('code', 'bad', 'err', 'Nom invalide'); end if;
      update mk_schools set name = left(trim(p->>'name'), 80) where id = sc.id;
      update mk_records set data = data || jsonb_build_object('name', left(trim(p->>'name'), 80)),
        rev = (select rev + 1 from mk_schools where id = sc.id), updated_by = 'propriétaire', updated_at = now()
        where school_id = sc.id and k = 'school' and id = 'school';
      update mk_schools set rev = rev + 1 where id = sc.id;
    end if;
    if p ? 'cycles' then
      if jsonb_typeof(p->'cycles') <> 'array' or jsonb_array_length(p->'cycles') = 0
         or exists (select 1 from jsonb_array_elements_text(p->'cycles') x where x not in ('jardin','fond','sec','lycee','pro')) then
        return jsonb_build_object('code', 'bad', 'err', 'Cycles invalides');
      end if;
      update mk_records set data = data || jsonb_build_object('cycles', p->'cycles'),
        rev = (select rev + 1 from mk_schools where id = sc.id), updated_by = 'propriétaire', updated_at = now()
        where school_id = sc.id and k = 'school' and id = 'school';
      update mk_schools set rev = rev + 1 where id = sc.id;
    end if;
    insert into mk_audit (school_id, who, action, info) values (sc.id, 'propriétaire', 'school_set', left((p - 'pw' - 'pin')::text, 150));
    return jsonb_build_object('ok', true);

  elsif act = 'school_delete' then
    select * into sc from mk_schools where code = v_code;
    if sc.id is null then return jsonb_build_object('code', 'bad', 'err', 'École introuvable'); end if;
    if upper(coalesce(p->>'confirm', '')) <> sc.code then return jsonb_build_object('code', 'bad', 'err', 'Tapez le code de l''école pour confirmer'); end if;
    delete from mk_schools where id = sc.id;
    return jsonb_build_object('ok', true);

  elsif act = 'reset_admin' then
    select * into sc from mk_schools where code = v_code;
    if sc.id is null then return jsonb_build_object('code', 'bad', 'err', 'École introuvable'); end if;
    if v_pin !~ '^\d{4,8}$' then return jsonb_build_object('code', 'bad', 'err', 'Le PIN doit avoir 4 à 8 chiffres'); end if;
    select * into v_adm from mk_creds where school_id = sc.id and role = 'admin' and not off
      and (p->>'login' is null or login = v_login) order by login limit 1;
    if v_adm.login is null then return jsonb_build_object('code', 'bad', 'err', 'Administrateur introuvable'); end if;
    update mk_creds set hash = crypt(v_pin, gen_salt('bf', 8)), must_change = true where school_id = sc.id and login = v_adm.login;
    delete from mk_sessions where school_id = sc.id and login = v_adm.login;
    delete from mk_attempts where akey = sc.code || ':' || v_adm.login;
    insert into mk_audit (school_id, who, action, info) values (sc.id, 'propriétaire', 'admin_pin_reset', v_adm.login);
    return jsonb_build_object('ok', true, 'login', v_adm.login);
  end if;
  return jsonb_build_object('code', 'bad', 'err', 'Action inconnue');
exception when others then
  return mki_fail(sqlerrm);
end $$;
-- ---------- Sauvegarde signée (export) et restauration ----------
-- Le fichier de sauvegarde est signé par le serveur (HMAC avec une clé secrète que personne ne connaît).
-- Toute modification du fichier rend la signature fausse : la restauration est alors refusée.
create or replace function mk_export(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  s mk_sessions; sc mk_schools; x mk_xstate; cfg mk_config; v_step text := coalesce(p->>'step', 'next');
  rr record; recs jsonb := '[]'::jsonb; cnt int := 0; v_lim int := least(greatest(coalesce((p->>'lim')::int, 1000), 10), 1000); v_more boolean := false; v_at text; v_sig text;
begin
  s := mki_auth(p->>'token');
  if s.role <> 'admin' then return jsonb_build_object('code', 'deny', 'err', 'Seul l''administrateur peut sauvegarder'); end if;
  select * into sc from mk_schools where id = s.school_id;
  if v_step = 'start' then
    delete from mk_xstate where school_id = s.school_id;
    insert into mk_xstate (school_id, kind, xid) values (s.school_id, 'export', mki_uid()) returning * into x;
    return jsonb_build_object('xid', x.xid);
  end if;
  select * into x from mk_xstate where school_id = s.school_id and kind = 'export' and xid = coalesce(p->>'xid', '');
  if x.xid is null then return jsonb_build_object('code', 'bad', 'err', 'Sauvegarde interrompue, recommencez'); end if;
  for rr in select k, id, y, data from mk_records where school_id = s.school_id and not del and (k, id) > (x.lk, x.lid)
           order by k, id limit v_lim + 1 loop
    cnt := cnt + 1;
    if cnt > v_lim then v_more := true; exit; end if;
    recs := recs || jsonb_build_array(jsonb_build_object('k', rr.k, 'id', rr.id, 'y', rr.y, 'd', rr.data));
    x.h := encode(digest(x.h || mki_recdigest(rr.k, rr.id, rr.y, rr.data), 'sha256'), 'hex');
    x.n := x.n + 1; x.lk := rr.k; x.lid := rr.id;
  end loop;
  update mk_xstate set h = x.h, n = x.n, lk = x.lk, lid = x.lid where school_id = s.school_id;
  if v_more then return jsonb_build_object('recs', recs, 'done', false); end if;
  select * into cfg from mk_config where id = 1;
  v_at := to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"');
  v_sig := encode(hmac(sc.code || '|' || v_at || '|' || x.n || '|' || x.h, cfg.secret, 'sha256'), 'hex');
  update mk_schools set last_backup = now() where id = s.school_id;
  delete from mk_xstate where school_id = s.school_id;
  perform mki_audit(s, 'backup', null, null, x.n || ' enregistrements');
  return jsonb_build_object('recs', recs, 'done', true, 'sig', v_sig, 'at', v_at, 'n', x.n, 'school', sc.code, 'fmt', 'kalanso-backup-2');
exception when others then
  return mki_fail(sqlerrm);
end $$;

create or replace function mk_restore(p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  s mk_sessions; sc mk_schools; x mk_xstate; cfg mk_config; v_step text := coalesce(p->>'step', '');
  e jsonb; v_ord bigint; v_left int; v_rev bigint; v_n int; v_exp text; rr record;
begin
  s := mki_auth(p->>'token');
  if s.role <> 'admin' then return jsonb_build_object('code', 'deny', 'err', 'Seul l''administrateur peut restaurer'); end if;
  select * into sc from mk_schools where id = s.school_id for update;
  if not mki_writable(sc.id) then return jsonb_build_object('code', 'readonly', 'err', 'Compte en lecture seule'); end if;

  if v_step = 'start' then
    delete from mk_xstate where school_id = s.school_id;
    delete from mk_stage where school_id = s.school_id;
    insert into mk_xstate (school_id, kind, xid, rev0) values (s.school_id, 'restore', mki_uid(), sc.rev) returning * into x;
    return jsonb_build_object('xid', x.xid);
  end if;
  select * into x from mk_xstate where school_id = s.school_id and kind = 'restore' and xid = coalesce(p->>'xid', '');
  if x.xid is null then return jsonb_build_object('code', 'bad', 'err', 'Restauration interrompue, recommencez'); end if;

  if v_step = 'stage' then
    if x.verified then return jsonb_build_object('code', 'bad', 'err', 'Déjà vérifié'); end if;
    if coalesce((p->>'i')::int, -1) <> x.next_i then raise exception 'MK:bad:Pages dans le désordre'; end if;
    if jsonb_typeof(p->'recs') <> 'array' or jsonb_array_length(p->'recs') > 1000 then raise exception 'MK:bad:Page invalide'; end if;
    for e in select value from jsonb_array_elements(p->'recs') loop
      if (e->>'k') not in ('school','class','subject','cs','teacher','student','user','grade','att','payment','exp','slot','pub','opt')
         or (e->>'id') !~ '^[A-Za-z0-9_:.-]{1,100}$' or jsonb_typeof(e->'d') <> 'object' then
        raise exception 'MK:bad:Fichier de sauvegarde invalide';
      end if;
      insert into mk_stage (school_id, ord, k, id, y, data) values (s.school_id, x.n, e->>'k', e->>'id', e->>'y', e->'d');
      x.h := encode(digest(x.h || mki_recdigest(e->>'k', e->>'id', e->>'y', e->'d'), 'sha256'), 'hex');
      x.n := x.n + 1;
    end loop;
    update mk_xstate set h = x.h, n = x.n, next_i = x.next_i + 1 where school_id = s.school_id;
    return jsonb_build_object('ok', true, 'n', x.n);

  elsif v_step = 'verify' then
    select * into cfg from mk_config where id = 1;
    v_exp := encode(hmac(coalesce(p->>'school', '') || '|' || coalesce(p->>'at', '') || '|' || x.n || '|' || x.h, cfg.secret, 'sha256'), 'hex');
    if coalesce(p->>'school', '') <> sc.code or (p->>'n')::bigint <> x.n or v_exp <> coalesce(p->>'sig', '') then
      delete from mk_stage where school_id = s.school_id;
      delete from mk_xstate where school_id = s.school_id;
      perform mki_audit(s, 'restore_refused', null, null, 'signature invalide');
      return jsonb_build_object('code', 'sig', 'err', 'Ce fichier a été modifié ou ne vient pas de votre école : restauration refusée');
    end if;
    if not exists (select 1 from mk_stage where school_id = s.school_id and k = 'school') then
      raise exception 'MK:bad:Sauvegarde incomplète';
    end if;
    update mk_xstate set verified = true where school_id = s.school_id;
    select count(*) into v_left from mk_stage where school_id = s.school_id;
    return jsonb_build_object('ok', true, 'left', v_left);

  elsif v_step = 'apply' then
    if not x.verified then raise exception 'MK:bad:Fichier non vérifié'; end if;
    v_rev := sc.rev; v_n := 0;
    for rr in select * from mk_stage where school_id = s.school_id order by ord limit 400 loop
      v_rev := v_rev + 1; v_n := v_n + 1;
      insert into mk_records (school_id, k, id, y, data, del, rev, updated_by)
        values (s.school_id, rr.k, rr.id, rr.y, rr.data, false, v_rev, 'restauration')
      on conflict (school_id, k, id) do update set y = excluded.y, data = excluded.data, del = false, rev = excluded.rev,
        updated_by = excluded.updated_by, updated_at = now();
      delete from mk_stage where school_id = s.school_id and ord = rr.ord;
    end loop;
    update mk_schools set rev = v_rev where id = s.school_id;
    select count(*) into v_left from mk_stage where school_id = s.school_id;
    return jsonb_build_object('ok', true, 'left', v_left);

  elsif v_step = 'finish' then
    if not x.verified then raise exception 'MK:bad:Fichier non vérifié'; end if;
    if exists (select 1 from mk_stage where school_id = s.school_id) then raise exception 'MK:bad:Restauration incomplète'; end if;
    v_rev := sc.rev;
    -- ce qui n'était pas dans la sauvegarde disparaît (sauf le compte de l'administrateur en cours)
    for rr in select k, id from mk_records where school_id = s.school_id and not del and rev <= x.rev0
               and not (k = 'user' and id = s.ref_id and s.kind = 'user') and k <> 'school' loop
      v_rev := v_rev + 1;
      update mk_records set del = true, rev = v_rev, updated_by = 'restauration', updated_at = now()
        where school_id = s.school_id and k = rr.k and id = rr.id;
    end loop;
    -- comptes de connexion : alignés sur les enregistrements restaurés (les PIN inconnus sont à redéfinir)
    delete from mk_creds c where c.school_id = s.school_id and not (c.kind = 'user' and c.ref_id = s.ref_id and s.kind = 'user')
      and not exists (select 1 from mk_records r where r.school_id = s.school_id and r.k = c.kind and r.id = c.ref_id and not r.del);
    for rr in select * from mk_records where school_id = s.school_id and k = 'user' and not del loop
      if not exists (select 1 from mk_creds c where c.school_id = s.school_id and c.kind = 'user' and c.ref_id = rr.id) then
        insert into mk_creds (school_id, login, hash, kind, ref_id, role, perms, cycles, must_change, teacher_id, name, off)
        values (s.school_id, rr.data->>'login', crypt(mki_uid(), gen_salt('bf', 8)), 'user', rr.id, rr.data->>'role',
                array(select jsonb_array_elements_text(rr.data->'perms')), array(select jsonb_array_elements_text(coalesce(rr.data->'cycles', '[]'::jsonb))),
                true, rr.data->>'teacherId', rr.data->>'name', coalesce((rr.data->>'off')::boolean, false))
        on conflict (school_id, login) do nothing;
      else
        update mk_creds set login = rr.data->>'login', role = rr.data->>'role', perms = array(select jsonb_array_elements_text(rr.data->'perms')),
          cycles = array(select jsonb_array_elements_text(coalesce(rr.data->'cycles', '[]'::jsonb))),
          teacher_id = rr.data->>'teacherId', name = rr.data->>'name', off = coalesce((rr.data->>'off')::boolean, false)
          where school_id = s.school_id and kind = 'user' and ref_id = rr.id;
      end if;
    end loop;
    for rr in select * from mk_records where school_id = s.school_id and k = 'student' and not del loop
      if not exists (select 1 from mk_creds c where c.school_id = s.school_id and c.kind = 'student' and c.ref_id = rr.id) then
        insert into mk_creds (school_id, login, hash, kind, ref_id, role, perms, name, off)
        values (s.school_id, lower(rr.data->>'matricule'), crypt(mki_uid(), gen_salt('bf', 8)), 'student', rr.id, 'eleve', array['eleve'],
                rr.data->>'name', coalesce((rr.data->>'left')::boolean, false))
        on conflict (school_id, login) do nothing;
      else
        update mk_creds set name = rr.data->>'name', off = coalesce((rr.data->>'left')::boolean, false)
          where school_id = s.school_id and kind = 'student' and ref_id = rr.id;
      end if;
    end loop;
    delete from mk_sessions where school_id = s.school_id and not (th = s.th);
    update mk_schools set rev = v_rev + 1 where id = s.school_id;
    -- l'enregistrement « école » reçoit une nouvelle révision pour forcer tous les appareils à se resynchroniser
    update mk_records set rev = v_rev + 1 where school_id = s.school_id and k = 'school' and id = 'school';
    delete from mk_xstate where school_id = s.school_id;
    perform mki_audit(s, 'restore', null, null, 'sauvegarde restaurée');
    return jsonb_build_object('ok', true, 'rev', v_rev + 1);
  end if;
  return jsonb_build_object('code', 'bad', 'err', 'Étape inconnue');
exception when others then
  return mki_fail(sqlerrm);
end $$;
-- ---------- Sécurité : seules les fonctions publiques sont appelables depuis Internet ----------
do $$
declare f record; r text;
begin
  for f in select p.oid::regprocedure as sig, p.proname as nm from pg_proc p
           where p.pronamespace = 'public'::regnamespace and (p.proname like 'mk\_%' or p.proname like 'mki\_%') loop
    execute format('revoke all on function %s from public', f.sig);
    for r in select rolname from pg_roles where rolname in ('anon', 'authenticated') loop
      execute format('revoke all on function %s from %I', f.sig, r);
      if f.nm in ('mk_ping', 'mk_login', 'mk_pull', 'mk_push', 'mk_logout', 'mk_change_pin',
                  'mk_owner', 'mk_audit_list', 'mk_export', 'mk_restore') then
        execute format('grant execute on function %s to %I', f.sig, r);
      end if;
    end loop;
  end loop;
  for f in select tablename from pg_tables where schemaname = 'public' and tablename like 'mk\_%' loop
    for r in select rolname from pg_roles where rolname in ('anon', 'authenticated') loop
      execute format('revoke all on table public.%I from %I', f.tablename, r);
    end loop;
  end loop;
end $$;
