package rs.raf.banka2_bek.dividend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.banka2_bek.dividend.dto.DividendPayoutDto;
import rs.raf.banka2_bek.dividend.service.DividendService;

import java.time.LocalDate;
import java.util.List;
// ============================================================
// TODO [B9 - Isplata dividendi na akcije | Nosilac: Djordje Zlatanovic]
//
// REST kontroler za pregled istorije dividendi.
// Base path: /dividends
//
// ZAVISNOSTI ZA INJEKTOVANJE:
//   - DividendService dividendService
//
// IMPLEMENTIRATI (endpoint metode):
//
//   GET /dividends/my  -> ResponseEntity<List<DividendPayoutDto>>
//       — Vraca sve dividende ulogovanog korisnika (CLIENT ili EMPLOYEE),
//         sortirano paymentDate DESC. Autentifikovano (authenticated()).
//         Delegira na dividendService.getMyDividendHistory().
//
//   GET /dividends/by-position/{portfolioId}  -> ResponseEntity<List<DividendPayoutDto>>
//       — Vraca istoriju dividendi za konkretnu Portfolio poziciju ({portfolioId}).
//         Autentifikovano. Delegira na dividendService.getDividendHistoryByPosition(portfolioId).
//         Vraca 403 ako ulogovani korisnik ne poseduje tu poziciju.
//
//   GET /admin/dividends  -> ResponseEntity<Page<DividendPayoutDto>>
//         (preporuka: staviti u zaseban @RestController na /admin/dividends da
//          odgovara konvenciji SavingsAdminController-a; moze i ovde kao
//          @GetMapping("/admin") uz prethodni @RequestMapping("/dividends"))
//       — Admin/supervisor pregled svih isplata paginiran.
//         @RequestParam(required=false) LocalDate from, to za filtrirani opseg.
//         Pristup: ADMIN i SUPERVISOR (@PreAuthorize ili GlobalSecurityConfig matcher).
//
// SECURITY INTEGRACIJA (ne implementirati ovde, naznaciti za koordinatora):
//   U GlobalSecurityConfig dodati (po analogiji sa /savings/** pravilima):
//     .requestMatchers(HttpMethod.GET, "/dividends/my", "/dividends/by-position/**").authenticated()
//     .requestMatchers(HttpMethod.GET, "/admin/dividends/**").hasAnyAuthority("ROLE_ADMIN","ADMIN","SUPERVISOR")
//
// Konvencija: pratiti paket `savings` (SavingsDepositController) kao sablon.
// Spec: Zadaci_Backend.pdf, zadatak B9.
// ============================================================

@RestController
@RequestMapping("/dividends")
@RequiredArgsConstructor
public class DividendController {
    private final DividendService dividendService;

    @GetMapping("/my")
    public ResponseEntity<List<DividendPayoutDto>> getMyDividendHistory() {
        return ResponseEntity.ok(dividendService.getMyDividendHistory());
    }

    @GetMapping("/by-position/{portfolioId}")
    public ResponseEntity<List<DividendPayoutDto>> getDividendHistoryByPosition(@PathVariable Long portfolioId) {
        return ResponseEntity.ok(dividendService.getDividendHistoryByPosition(portfolioId));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<DividendPayoutDto>> getAdminDividends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(dividendService.getAdminDividendHistory(from, to, PageRequest.of(page, size)));
    }
}
