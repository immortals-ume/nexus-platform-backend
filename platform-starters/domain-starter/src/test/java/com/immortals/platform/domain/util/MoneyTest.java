package com.immortals.platform.domain.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for Money value object.
 */
class MoneyTest {

    @Test
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        BigDecimal amount = new BigDecimal("100.50");
        Currency currency = Currency.getInstance("USD");
        
        Money money = new Money(amount, currency);
        
        assertThat(money.amount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(currency);
    }

    @Test
    void shouldRoundAmountToTwoDecimalPlaces() {
        BigDecimal amount = new BigDecimal("100.567");
        Currency currency = Currency.getInstance("USD");
        
        Money money = new Money(amount, currency);
        
        assertThat(money.amount()).isEqualTo(new BigDecimal("100.57"));
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        Currency currency = Currency.getInstance("USD");
        
        assertThatThrownBy(() -> new Money(null, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot be null");
    }

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-10.00");
        Currency currency = Currency.getInstance("USD");
        
        assertThatThrownBy(() -> new Money(negativeAmount, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot be negative");
    }

    @Test
    void shouldThrowExceptionForNullCurrency() {
        BigDecimal amount = new BigDecimal("100.00");
        
        assertThatThrownBy(() -> new Money(amount, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency cannot be null");
    }

    @Test
    void shouldCreateUsdMoney() {
        Money money = Money.usd(new BigDecimal("50.25"));
        
        assertThat(money.amount()).isEqualTo(new BigDecimal("50.25"));
        assertThat(money.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldCreateUsdMoneyFromDouble() {
        Money money = Money.usd(75.99);
        
        assertThat(money.amount()).isEqualTo(new BigDecimal("75.99"));
        assertThat(money.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldCreateMoneyWithCurrencyCode() {
        Money money = Money.of(new BigDecimal("200.00"), "EUR");
        
        assertThat(money.amount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(money.currency()).isEqualTo(Currency.getInstance("EUR"));
    }

    @Test
    void shouldCreateZeroMoney() {
        Currency currency = Currency.getInstance("GBP");
        Money money = Money.zero(currency);
        
        assertThat(money.amount()).isEqualTo(BigDecimal.ZERO);
        assertThat(money.currency()).isEqualTo(currency);
        assertThat(money.isZero()).isTrue();
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money money1 = Money.usd(new BigDecimal("100.00"));
        Money money2 = Money.usd(new BigDecimal("50.25"));
        
        Money result = money1.add(money2);
        
        assertThat(result.amount()).isEqualTo(new BigDecimal("150.25"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money usdMoney = Money.usd(new BigDecimal("100.00"));
        Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");
        
        assertThatThrownBy(() -> usdMoney.add(eurMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot add money with different currencies");
    }

    @Test
    void shouldSubtractMoneyWithSameCurrency() {
        Money money1 = Money.usd(new BigDecimal("100.00"));
        Money money2 = Money.usd(new BigDecimal("30.25"));
        
        Money result = money1.subtract(money2);
        
        assertThat(result.amount()).isEqualTo(new BigDecimal("69.75"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
        Money usdMoney = Money.usd(new BigDecimal("100.00"));
        Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");
        
        assertThatThrownBy(() -> usdMoney.subtract(eurMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot subtract money with different currencies");
    }

    @Test
    void shouldMultiplyByBigDecimal() {
        Money money = Money.usd(new BigDecimal("100.00"));
        BigDecimal factor = new BigDecimal("1.5");
        
        Money result = money.multiply(factor);
        
        assertThat(result.amount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldMultiplyByInteger() {
        Money money = Money.usd(new BigDecimal("25.50"));
        
        Money result = money.multiply(3);
        
        assertThat(result.amount()).isEqualTo(new BigDecimal("76.50"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldDivideByBigDecimal() {
        Money money = Money.usd(new BigDecimal("100.00"));
        BigDecimal divisor = new BigDecimal("4");
        
        Money result = money.divide(divisor);
        
        assertThat(result.amount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void shouldThrowExceptionWhenDividingByZero() {
        Money money = Money.usd(new BigDecimal("100.00"));
        
        assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot divide by zero");
    }

    @Test
    void shouldCompareMoneyAmounts() {
        Money money1 = Money.usd(new BigDecimal("100.00"));
        Money money2 = Money.usd(new BigDecimal("50.00"));
        Money money3 = Money.usd(new BigDecimal("150.00"));
        
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money1.isLessThan(money3)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isFalse();
        assertThat(money3.isLessThan(money1)).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenComparingDifferentCurrencies() {
        Money usdMoney = Money.usd(new BigDecimal("100.00"));
        Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");
        
        assertThatThrownBy(() -> usdMoney.isGreaterThan(eurMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot compare money with different currencies");
        
        assertThatThrownBy(() -> usdMoney.isLessThan(eurMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot compare money with different currencies");
    }

    @Test
    void shouldCheckIfMoneyIsZero() {
        Money zeroMoney = Money.usd(BigDecimal.ZERO);
        Money nonZeroMoney = Money.usd(new BigDecimal("10.00"));
        
        assertThat(zeroMoney.isZero()).isTrue();
        assertThat(nonZeroMoney.isZero()).isFalse();
    }

    @Test
    void shouldCheckIfMoneyIsPositive() {
        Money zeroMoney = Money.usd(BigDecimal.ZERO);
        Money positiveMoney = Money.usd(new BigDecimal("10.00"));
        
        assertThat(zeroMoney.isPositive()).isFalse();
        assertThat(positiveMoney.isPositive()).isTrue();
    }

    @Test
    void shouldReturnCurrencyCode() {
        Money usdMoney = Money.usd(new BigDecimal("100.00"));
        Money eurMoney = Money.of(new BigDecimal("100.00"), "EUR");
        
        assertThat(usdMoney.getCurrencyCode()).isEqualTo("USD");
        assertThat(eurMoney.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void shouldFormatToString() {
        Money usdMoney = Money.usd(new BigDecimal("123.45"));
        
        String formatted = usdMoney.toString();
        
        assertThat(formatted).contains("123.45");
        assertThat(formatted).contains("$"); // USD symbol
    }

    @Test
    void shouldBeImmutable() {
        Money original = Money.usd(new BigDecimal("100.00"));
        Money added = original.add(Money.usd(new BigDecimal("50.00")));
        
        // Original should remain unchanged
        assertThat(original.amount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(added.amount()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void shouldSupportEquality() {
        Money money1 = Money.usd(new BigDecimal("100.00"));
        Money money2 = Money.usd(new BigDecimal("100.00"));
        Money money3 = Money.usd(new BigDecimal("200.00"));
        Money eurMoney = Money.of(new BigDecimal("100.00"), "EUR");
        
        assertThat(money1).isEqualTo(money2);
        assertThat(money1).isNotEqualTo(money3);
        assertThat(money1).isNotEqualTo(eurMoney);
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }
}