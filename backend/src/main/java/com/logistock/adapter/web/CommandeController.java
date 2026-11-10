package com.logistock.adapter.web;

import com.logistock.adapter.pdf.FacturePdf;
import com.logistock.adapter.web.dto.CommandeRequest;
import com.logistock.adapter.web.dto.CommandeResponse;
import com.logistock.adapter.web.dto.FactureResponse;
import com.logistock.domain.model.StatutCommande;
import com.logistock.usecase.CommandeUseCase;
import com.logistock.usecase.FactureUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private final CommandeUseCase commandes;
    private final FactureUseCase factures;

    public CommandeController(CommandeUseCase commandes, FactureUseCase factures) {
        this.commandes = commandes;
        this.factures = factures;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeResponse creer(@Valid @RequestBody CommandeRequest requete, Authentication authentication) {
        return CommandeResponse.from(
                commandes.creer(requete.clientId(), requete.toDemandes(), authentication.getName()));
    }

    @GetMapping
    public List<CommandeResponse> lister(@RequestParam(required = false) StatutCommande statut,
                                         @RequestParam(required = false) Long clientId) {
        return commandes.lister(statut, clientId).stream().map(CommandeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CommandeResponse trouver(@PathVariable Long id) {
        return CommandeResponse.from(commandes.trouver(id));
    }

    @PostMapping("/{id}/validation")
    public CommandeResponse valider(@PathVariable Long id, Authentication authentication) {
        return CommandeResponse.from(commandes.valider(id, authentication.getName()));
    }

    @PostMapping("/{id}/annulation")
    public CommandeResponse annuler(@PathVariable Long id) {
        return CommandeResponse.from(commandes.annuler(id));
    }

    @GetMapping("/{id}/facture")
    public FactureResponse facture(@PathVariable Long id) {
        return FactureResponse.from(factures.parCommande(id));
    }

    @GetMapping("/{id}/facture.pdf")
    public ResponseEntity<byte[]> facturePdf(@PathVariable Long id) {
        FactureUseCase.Detail detail = factures.parCommande(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + detail.facture().numero() + ".pdf\"")
                .body(FacturePdf.generer(detail));
    }
}
