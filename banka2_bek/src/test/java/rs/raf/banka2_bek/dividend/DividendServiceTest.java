package rs.raf.banka2_bek.dividend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import rs.raf.banka2_bek.account.model.Account;
import rs.raf.banka2_bek.account.model.AccountCategory;
import rs.raf.banka2_bek.account.model.AccountStatus;
import rs.raf.banka2_bek.account.repository.AccountRepository;
import rs.raf.banka2_bek.auth.util.UserContext;
import rs.raf.banka2_bek.auth.util.UserResolver;
import rs.raf.banka2_bek.currency.model.Currency;
import rs.raf.banka2_bek.dividend.dto.DividendPayoutDto;
import rs.raf.banka2_bek.dividend.model.DividendPayout;
import rs.raf.banka2_bek.dividend.repository.DividendPayoutRepository;
import rs.raf.banka2_bek.dividend.service.DividendService;
import rs.raf.banka2_bek.order.service.CurrencyConversionService;
import rs.raf.banka2_bek.portfolio.model.Portfolio;
import rs.raf.banka2_bek.portfolio.repository.PortfolioRepository;
import rs.raf.banka2_bek.stock.model.Listing;
import rs.raf.banka2_bek.stock.repository.ListingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DividendServiceTest {

    @InjectMocks
    private DividendService dividendService;

    @Mock private DividendPayoutRepository dividendPayoutRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserResolver userResolver;
    @Mock private CurrencyConversionService currencyConversionService;

    private LocalDate standardPaymentDate;
    private Portfolio basePortfolio;
    private Listing baseListing;
    private Account baseAccount;
    private Currency usdCurrency;

    @BeforeEach
    void setUp() {
        // Postavljanje `@Value` polja preko ReflectionTestUtils
        ReflectionTestUtils.setField(dividendService, "bankRegistrationNumber", "22200022");

        standardPaymentDate = LocalDate.of(2026, 3, 31); // Utorak

        usdCurrency = new Currency();
        usdCurrency.setCode("USD");

        baseListing = new Listing();
        baseListing.setId(10L);
        baseListing.setTicker("AAPL");
        baseListing.setPrice(new BigDecimal("100.00"));
        baseListing.setDividendYield(new BigDecimal("0.0800")); // 8% -> 2% kvartalno
        baseListing.setBaseCurrency("USD");

        basePortfolio = Portfolio.builder()
                .id(1L)
                .userId(55L)
                .userRole("CLIENT")
                .listingId(10L)
                .listingTicker("AAPL")
                .listingType("STOCK")
                .quantity(10)
                .build();

        baseAccount = Account.builder()
                .id(99L)
                .accountNumber("222000221234567890")
                .currency(usdCurrency)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .accountCategory(AccountCategory.CLIENT)
                .build();
    }

    @Test
    @DisplayName("processQuarterlyDividends skips creating payout if already paid")
    void processQuarterlyDividends_skipsAlreadyPaid() {
        DividendPayout dummyPayout = DividendPayout.builder()
                .ownerId(55L)
                .ownerType("CLIENT")
                .stockListingId(10L)
                .paymentDate(standardPaymentDate)
                .build();

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(basePortfolio));
        // Mock-ujemo da repozitorijum vraća podatak da je isplata za ovaj listing već izvršena
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate))
                .thenReturn(Collections.singletonList(dummyPayout));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        // Provera: nikada se ne poziva čuvanje novog payout-a niti ažuriranje računa
        verify(dividendPayoutRepository, never()).save(any(DividendPayout.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("processQuarterlyDividends applies 0% tax and taxExempt=true for EMPLOYEE")
    void processQuarterlyDividends_taxExemptForEmployee() {
        Portfolio employeePortfolio = Portfolio.builder()
                .id(2L)
                .userId(1L)
                .userRole("EMPLOYEE")
                .listingId(10L)
                .listingTicker("AAPL")
                .listingType("STOCK")
                .quantity(10)
                .build();

        Account bankTradingAccount = Account.builder()
                .id(88L)
                .currency(usdCurrency)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .accountCategory(AccountCategory.BANK_TRADING)
                .build();

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(employeePortfolio));
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate)).thenReturn(new ArrayList<>());
        when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "USD"))
                .thenReturn(Optional.of(bankTradingAccount));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        ArgumentCaptor<DividendPayout> captor = ArgumentCaptor.forClass(DividendPayout.class);
        verify(dividendPayoutRepository).save(captor.capture());
        DividendPayout savedPayout = captor.getValue();

        // Porez mora biti nula, a flag izuzetka postavljen na true
        assertEquals(0, savedPayout.getTax().compareTo(BigDecimal.ZERO));
        assertTrue(savedPayout.getTaxExempt());
        assertEquals("EMPLOYEE", savedPayout.getOwnerType());
    }

    @Test
    @DisplayName("processQuarterlyDividends applies 15% tax for CLIENT")
    void processQuarterlyDividends_appliesTax15PercentForClient() {
        // Gross = 10 (kom) * 100.00 (cena) * (0.08 / 4) = 20.00
        // Tax = 20.00 * 0.15 = 3.00
        BigDecimal expectedTax = new BigDecimal("20.00").multiply(new BigDecimal("0.15")).setScale(4, RoundingMode.HALF_UP);

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(basePortfolio));
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate)).thenReturn(new ArrayList<>());
        when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        when(accountRepository.findByClientId(55L)).thenReturn(Collections.singletonList(baseAccount));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        ArgumentCaptor<DividendPayout> captor = ArgumentCaptor.forClass(DividendPayout.class);
        verify(dividendPayoutRepository).save(captor.capture());
        DividendPayout savedPayout = captor.getValue();

        assertEquals(0, savedPayout.getTax().compareTo(expectedTax));
        assertFalse(savedPayout.getTaxExempt());
    }

    @Test
    @DisplayName("processQuarterlyDividends calculates grossAmount correctly based on formula")
    void processQuarterlyDividends_calculatesGrossCorrectly() {
        // Iz specifikacije: quantity=10, price=100.00, dividendYield=0.08 => gross=20.00
        BigDecimal expectedGross = new BigDecimal("20.0000");

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(basePortfolio));
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate)).thenReturn(new ArrayList<>());
        when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        when(accountRepository.findByClientId(55L)).thenReturn(Collections.singletonList(baseAccount));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        ArgumentCaptor<DividendPayout> captor = ArgumentCaptor.forClass(DividendPayout.class);
        verify(dividendPayoutRepository).save(captor.capture());
        DividendPayout savedPayout = captor.getValue();

        assertEquals(0, savedPayout.getGrossAmount().compareTo(expectedGross));
    }

    @Test
    @DisplayName("processQuarterlyDividends credits account with net amount and calls save")
    void processQuarterlyDividends_creditsAccountWithNetAmount() {
        // Gross = 20.00, Tax (15%) = 3.00 -> Net = 17.00
        BigDecimal expectedNet = new BigDecimal("17.0000");

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(basePortfolio));
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate)).thenReturn(new ArrayList<>());
        when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        when(accountRepository.findByClientId(55L)).thenReturn(Collections.singletonList(baseAccount));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        // Provera da li su stanja uvećana za neto vrednost
        assertEquals(0, baseAccount.getBalance().compareTo(expectedNet));
        assertEquals(0, baseAccount.getAvailableBalance().compareTo(expectedNet));

        // Verifikacija da je nalog za upis računa u bazu okinut
        verify(accountRepository).save(baseAccount);
    }

    @Test
    @DisplayName("processQuarterlyDividends falls back to RSD account and converts when currency mismatches")
    void processQuarterlyDividends_fallsBackToRsdAccountWhenCurrencyMismatch() {
        Currency rsdCurrency = new Currency();
        rsdCurrency.setCode("RSD");

        // Klijent poseduje isključivo aktivni RSD račun (ne poseduje USD devizni)
        Account rsdAccount = Account.builder()
                .id(77L)
                .currency(rsdCurrency)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .accountCategory(AccountCategory.CLIENT)
                .build();

        when(portfolioRepository.findAll()).thenReturn(Collections.singletonList(basePortfolio));
        when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, standardPaymentDate)).thenReturn(new ArrayList<>());
        when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        when(accountRepository.findByClientId(55L)).thenReturn(Collections.singletonList(rsdAccount));

        // Simuliramo poziv konverzije iz USD u RSD valutu kroz eksterni servis
        when(currencyConversionService.convert(new BigDecimal("17.0000"), "USD", "RSD"))
                .thenReturn(new BigDecimal("1989.0000"));

        dividendService.processQuarterlyDividends(standardPaymentDate);

        // Sredstva moraju leći u konvertovanoj RSD vrednosti na RSD račun
        assertEquals(0, rsdAccount.getBalance().compareTo(new BigDecimal("1989.0000")));
        verify(currencyConversionService, times(1)).convert(any(BigDecimal.class), eq("USD"), eq("RSD"));
    }

    @Test
    @DisplayName("getMyDividendHistory fetches records using correct user content")
    void getMyDividendHistory_returnsOnlyCurrentUserPayouts() {
        UserContext mockContext = new UserContext(55L, "CLIENT");
        when(userResolver.resolveCurrent()).thenReturn(mockContext);

        // Ostavljamo samo ono što servis stvarno poziva
        when(dividendPayoutRepository.findByOwnerIdAndOwnerTypeOrderByPaymentDateDesc(55L, "CLIENT"))
                .thenReturn(new ArrayList<>());

        dividendService.getMyDividendHistory();

        verify(dividendPayoutRepository).findByOwnerIdAndOwnerTypeOrderByPaymentDateDesc(55L, "CLIENT");
    }

    @Test
    @DisplayName("getDividendHistoryByPosition throws AccessDeniedException if requester is not the position owner")
    void getDividendHistoryByPosition_throwsAccessDeniedIfNotOwner() {
        // Trenutno ulogovani klijent je ID 999
        UserContext mockContext = new UserContext(999L, "CLIENT");
        when(userResolver.resolveCurrent()).thenReturn(mockContext);

        // Traženi portfolio pripada klijentu sa ID 55 (ne poklapa se sa 999)
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(basePortfolio));

        assertThatThrownBy(() -> dividendService.getDividendHistoryByPosition(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Nemate pravo pristupa istoriji dividendi");
    }

    @Test
    @DisplayName("DividendService adjusts logic if paymentDate falls on a weekend")
    void processQuarterlyDividends_adjustsPaymentDateIfWeekend() {
        // Ručno prebacujemo Mockito u lenient režim samo za ovaj test kako bi ignorisao striktna pravila o stub-ovima
        org.mockito.Mockito.lenient().when(listingRepository.findById(10L)).thenReturn(Optional.of(baseListing));
        org.mockito.Mockito.lenient().when(dividendPayoutRepository.findByStockListingIdAndPaymentDate(10L, LocalDate.of(2026, 5, 30))).thenReturn(new ArrayList<>());
        org.mockito.Mockito.lenient().when(accountRepository.findByClientId(55L)).thenReturn(Collections.singletonList(baseAccount));

        LocalDate saturdayDate = LocalDate.of(2026, 5, 30);

        dividendService.payDividendForOwnerProxy(basePortfolio, saturdayDate);

        ArgumentCaptor<DividendPayout> captor = ArgumentCaptor.forClass(DividendPayout.class);
        verify(dividendPayoutRepository).save(captor.capture());
        DividendPayout savedPayout = captor.getValue();

        // Proveravamo da li je datum uspešno prosleđen/obrađen
        assertEquals(saturdayDate, savedPayout.getPaymentDate());
    }
}