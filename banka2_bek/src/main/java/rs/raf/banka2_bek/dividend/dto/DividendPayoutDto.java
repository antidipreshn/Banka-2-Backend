package rs.raf.banka2_bek.dividend.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
// ============================================================
// TODO [B9 - Isplata dividendi na akcije | Nosilac: Djordje Zlatanovic]
//
// DTO za odgovor na GET /dividends/my i GET /dividends/by-position/{portfolioId}.
// Mapira se iz DividendPayout entiteta (dodati SavingsMapper-like klasu u
// dividend/mapper/DividendMapper.java ako budete dodavali mapper, ili mapirati inline u servisu).
//
// IMPLEMENTIRATI (polja koja klasa treba da ima):
//   - Long id
//   - Long ownerId
//   - String ownerType             — "CLIENT" ili "EMPLOYEE"
//   - Long stockListingId
//   - String stockTicker
//   - Integer quantity
//   - BigDecimal priceOnDate       — cena akcije na dan obracuna
//   - BigDecimal dividendYieldRate — kvartalni prinos (dividendYield / 4)
//   - BigDecimal grossAmount       — bruto iznos pre poreza
//   - BigDecimal tax               — iznos poreza (0 za EMPLOYEE)
//   - BigDecimal netAmount         — neto iznos koji je knjizen na racun
//   - Long creditedAccountId       — racun na koji je isplaceno
//   - String currencyCode          — valuta isplate
//   - LocalDate paymentDate        — datum isplate
//   - Boolean taxExempt            — true za EMPLOYEE (aktuar/bankin racun)
//   - LocalDateTime createdAt
//
// Dodati Lombok anotacije @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// kao sto radi SavingsDepositDto.
//
// Konvencija: pratiti paket `savings` kao sablon.
// Spec: Zadaci_Backend.pdf, zadatak B9.
// ============================================================
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendPayoutDto {
    private Long id;
    private Long ownerId;
    private String ownerType;
    private Long stockListingId;
    private String stockTicker;
    private Integer quantity;
    private BigDecimal priceOnDate;
    private BigDecimal dividendYieldRate;
    private BigDecimal grossAmount;
    private BigDecimal tax;
    private BigDecimal netAmount;
    private Long creditedAccountId;
    private String currencyCode;
    private LocalDate paymentDate;
    private Boolean taxExempt;
    private LocalDateTime createdAt;
}
