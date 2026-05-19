package rs.raf.banka2_bek.dividend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.banka2_bek.dividend.model.DividendPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.raf.banka2_bek.dividend.model.DividendPayout;

import java.time.LocalDate;
import java.util.List;
// ============================================================
// TODO [B9 - Isplata dividendi na akcije | Nosilac: Djordje Zlatanovic]
//
// JPA repozitorijum za DividendPayout entitet.
//
// IMPLEMENTIRATI (custom metode koje treba dodati u interfejs):
//   - List<DividendPayout> findByOwnerIdAndOwnerTypeOrderByPaymentDateDesc(Long ownerId, String ownerType)
//       — istorija dividendi po korisniku (CLIENT ili EMPLOYEE), za GET /dividends/my endpoint
//   - List<DividendPayout> findByStockListingIdAndPaymentDate(Long stockListingId, LocalDate paymentDate)
//       — provera idempotentnosti: da li je za dati listing i kvartal vec isplacena dividenda
//         (koristiti u DividendService pre kreiranja novog PayoutRecorda kako bi scheduler bio
//         siguran da ne isplati duplo u slucaju restarta)
//   - List<DividendPayout> findByOwnerIdAndOwnerTypeAndStockListingId(Long ownerId, String ownerType, Long stockListingId)
//       — istorija dividendi po vlasniku i po hartiji, za GET /dividends/by-position/{portfolioId} endpoint
//   - Page<DividendPayout> findAllByOrderByPaymentDateDesc(Pageable pageable)
//       — admin pregled svih isplata (paginiran), za GET /admin/dividends endpoint
//   - @Query: findByPaymentDateBetween(LocalDate from, LocalDate to)
//       — za filtrirani admin pregled po datumskom opsegu kvartala
//
// Konvencija: pratiti paket `savings` kao sablon.
// Spec: Zadaci_Backend.pdf, zadatak B9.
// ============================================================
@Repository
public interface DividendPayoutRepository extends JpaRepository<DividendPayout, Long> {
    List<DividendPayout> findByOwnerIdAndOwnerTypeOrderByPaymentDateDesc(Long ownerId, String ownerType);

    List<DividendPayout> findByStockListingIdAndPaymentDate(Long stockListingId, LocalDate paymentDate);

    List<DividendPayout> findByOwnerIdAndOwnerTypeAndStockListingId(Long ownerId, String ownerType, Long stockListingId);

    Page<DividendPayout> findAllByOrderByPaymentDateDesc(Pageable pageable);

    @Query("SELECT d FROM DividendPayout d WHERE d.paymentDate BETWEEN :from AND :to ORDER BY d.paymentDate DESC")
    List<DividendPayout> findByPaymentDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
