/*
 * Copyright 2012 Glenn Heinze .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accounted4.money.loan;

import com.accounted4.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Iterator;
import java.util.NoSuchElementException;



/**
 * Utility functions for amortization schedule generation
 * 
 * @author Glenn Heinze 
 */
public class AmortizationCalculator {
    

    /**
     * Generate an ordered list of payments forming an amortization schedule.
     *
     * If the payment is greater than the regular calculated amortization payment,
     * then the monthly surplus is used as extra principal payment.
     * 
     * If the payment is less than the regular monthly amortization payments,
     * then the supplied payment is ignored and a regular schedule is generated.
     *
     * @param terms
     * 
     * @return An ordered list of payments which comprise the set of regular
     * payments fulfilling the terms of the given amortization parameters.
     */
    public static Iterator<ScheduledPayment> getPayments(AmortizationAttributes terms) {
        
        return terms.isInterestOnly() ?
                new InterestOnlyIterator(terms) :
                new AmortizedIterator(terms);
        
    }
         
    
    
    /* ===============================
     * Abstract class holding supplied and calculated values required for calculating
     * subsequent payments, whether interest-only or amortized.
     */
    private static abstract class AmortizationIterator implements Iterator<ScheduledPayment> {

        // provided
        protected final AmortizationAttributes terms;
        
        // Short-cuts to provided "term" attributes
        protected final Money requestedPeriodicPayment;
        protected final Currency currency;
        protected final RoundingMode roundingMode;
        
        // A zero Money amount with the same currency and rounding mode as the loan amount
        protected final Money zeroMoney;
        
        // TODO: this only considers monthly payments but should handle periodic payments
        // (weekly, every two weeks, every two months, ...)
        
        
        // State members:
        
        /*
         * The calculatedMonthlyPayment is the computed monthly payment. It could be less
         * then the specified monthly payment if there is a desire to pay extra principal
         * during each payment period.
         */
        protected final Money calculatedPeriodicPayment;
        
        // Counter of payments already calculated
        protected int paymentNumber = 0;
        
        protected Money remainingBalance;

        
        public AmortizationIterator(AmortizationAttributes terms) {
            
            this.terms = terms;
            
            // Set short-cuts to input fiels
            this.requestedPeriodicPayment = terms.getRegularPayment();
            this.currency = terms.getLoanAmount().getCurrency();
            this.roundingMode = terms.getLoanAmount().getRoundingMode();
            
            // Initialize balance to loan amount
            this.remainingBalance = new Money(terms.getLoanAmount());

            double calculatedPaymentValue = terms.isInterestOnly() ?
                    
                    getInterestOnlyMonthlyPayment(terms.getLoanAmount().getAmount().doubleValue(), terms.getInterestRate()) :
                    
                    getAmortizedMonthlyPayment(
                        terms.getLoanAmount(),
                        terms.getInterestRate(),
                        terms.getCompoundingPeriodsPerYear(),
                        terms.getAmortizationPeriodMonths() );
            
            calculatedPeriodicPayment = new Money(BigDecimal.valueOf(calculatedPaymentValue), currency, roundingMode);
                    
            // The regular payment has to be at least the calculated payment, if not more
            assert !calculatedPeriodicPayment.greaterThan(requestedPeriodicPayment);
            
            zeroMoney = new Money("0", currency, roundingMode);
            
        }

        
        /**
         * True if there is still a balance to be paid and loan term has not yet been reached
         * 
         * @return 
         */
        @Override
        public boolean hasNext() {
            return  paymentNumber < terms.getTermInMonths() &&
                    remainingBalance.greaterThan(zeroMoney)
                    ;
        }

        
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator remove operation not supported.");
        }
        
    }

    
    
    
    /* =============================== */
    
    /**
     * Iterator for an interest-only payment schedule. By definition this is interest-only
     * so the payments will always be the calculated amount and any requested regular 
     * payment will be ignored.
     * 
     */
    public static class InterestOnlyIterator extends AmortizationIterator {

        public InterestOnlyIterator(AmortizationAttributes terms) {
            super(terms);
        }

        
        @Override
        public ScheduledPayment next() {
            
            if (paymentNumber >= terms.getTermInMonths()) {
                throw new NoSuchElementException("Attempting to retrieve payment beyond term.");
            }
            
            paymentNumber++;
            
            LocalDate date = terms.getAdjustmentDate().plusMonths(paymentNumber);

            ScheduledPayment payment = new ScheduledPayment();
            payment.setPaymentNumber(paymentNumber);
            payment.setPaymentDate(date);
            payment.setInterest(calculatedPeriodicPayment);

            // The regular payment is pure interest and it should be the same as the calculated payment
            payment.setPrincipal(zeroMoney);
            payment.setBalance(terms.getLoanAmount());
            
            return payment;
            
        }
        
    }
    
    
    /* =============================== */
    
    public static class AmortizedIterator extends AmortizationIterator {         
        
        private final double j;   // period rate: ie rate until compounding trigger (rate for 6 months for semi-annual, rate for 1 month for monthly)
        private final Money periodicPayment;
        
        public AmortizedIterator(AmortizationAttributes terms) {
            super(terms);
            
            assert terms.getLoanAmount().greaterThan(zeroMoney);
            assert terms.getTermInMonths() > 0;
            assert terms.getAmortizationPeriodMonths() > 0;
            assert terms.getCompoundingPeriodsPerYear() > 0;
            assert terms.getInterestRate() > 0.0d;
            
            j = getPeriodRate(terms.getInterestRate(), terms.getCompoundingPeriodsPerYear());

            periodicPayment = calculatedPeriodicPayment.greaterThan(requestedPeriodicPayment) ?
                    calculatedPeriodicPayment :
                    requestedPeriodicPayment;
            
        }
        
        
        @Override
        public ScheduledPayment next() {
            
            if (paymentNumber >= terms.getTermInMonths()) {
                throw new NoSuchElementException("Attempting to retrieve payment beyond term.");
            }
            
            paymentNumber++;

            LocalDate date = terms.getAdjustmentDate().plusMonths(paymentNumber);

            // Interest amounts rounding take precedence
            double computedInterest = remainingBalance.getAmount().doubleValue() * j;
            Money interest = new Money( BigDecimal.valueOf(computedInterest), currency, roundingMode);
            
            // The periodic payment is consistent, so anything that is not interest is principal
            Money principalMoney = periodicPayment.subtract(interest);
            if (principalMoney.greaterThan(remainingBalance)) {
                principalMoney = new Money(remainingBalance);
            }            
            remainingBalance = remainingBalance.subtract(principalMoney);
            
            ScheduledPayment payment = new ScheduledPayment();
            payment.setPaymentNumber(paymentNumber);
            payment.setPaymentDate(date);
            payment.setInterest(interest);
            payment.setPrincipal(principalMoney);
            payment.setBalance(remainingBalance);
            
            return payment;
            
        }

    }
    
    
    public static Money getMonthlyPayment(AmortizationAttributes amAttrs) {

        double monthlyPayment;
        
        if (amAttrs.isInterestOnly()) {
            monthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amAttrs.getLoanAmount().getAmount().doubleValue(), amAttrs.getInterestRate());
        } else {
            monthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(
                    amAttrs.getLoanAmount(),
                    amAttrs.getInterestRate(),
                    amAttrs.getCompoundingPeriodsPerYear(),
                    amAttrs.getAmortizationPeriodMonths()
                    );
        }
        
        return new Money(BigDecimal.valueOf(monthlyPayment));

    }

    
    private static final int DAYS_IN_A_YEAR = 365;

    /**
     * Daily interest rate for balance. Typically used to calculate the initial
     * adjustment amount or late payments on payout. Assumes a constant 365 days
     * per year, regardless of the year.
     * 
     * @param amount The balance upon which the rate is to be applied.
     * @param annualInterestRatePercent input annual interest rate as a percent (ie 8.25 for 8.25%)
     * @return 
     */
    public static Money getPerDiem(Money amount, double annualInterestRatePercent) {
        return amount.multiply(annualInterestRatePercent * 0.01 / DAYS_IN_A_YEAR);
    }
    
    
    /**
     * Daily interest rate for balance multiplied by the number of days. Note that this does not
     * calculate the interest for a period. It calculates it for a day and then multiplies out.
     * The difference is that fractional units will accumulate. For example, if the raw daily interest
     * rate were $32.111, this would be rounded up to a daily interest rate of $32.12. Applied 
     * for 100 days: $321.20, (not $32.111 * 10 days = $321.11).
     * 
     * @param amount The balance upon which the daily rate is to be applied.
     * @param annualInterestRatePercent input annual interest rate as a percent (ie 8.25 for 8.25%)
     * @param days Days for which the daily interest rate should be applied.
     * @return 
     */
    public static Money getAdjustmentAmount(Money amount, double annualInterestRatePercent, int days) {
        return getPerDiem(amount, annualInterestRatePercent).multiply(days);
    }
    
    
    /**
     * Given an amount and an annual interest rate, return the monthly payment
     * for an interest only loan.
     *
     * @param amount the principal amount
     * @param rate the annual interest rate expressed as a percent
     * @return Raw amount with fractional units representing
     * the monthly interest charge.
     */
    public static double getInterestOnlyMonthlyPayment(double amount, double rate) {
        // percent to decimal, annual rate to period (monthly) rate
        return amount * rate / 100. / 12.; 
    }
         

    
    /**
     * Given an amount and an annual interest rate, return the monthly payment
     * for an interest only loan.
     *
     * @param loanAmount the principal
     * @param i the interest rate expressed as a percent
     * @param compoundPeriodsPerYear  The number of times a year interest is calculated
     *     Canadian law specifies semi-annually (ie 2x a year).  Americans
     *     typically use monthly (ie 12x a year)
     * @param amortizationPeriod  The number of months the loan is spread over
     *
     * @return The expected monthly payment amortized over the given period.
     */
    public static double getAmortizedMonthlyPayment(
            Money  loanAmount,
            double i,
            int    compoundPeriodsPerYear,
            int    amortizationPeriod ) {
        
        double a = loanAmount.getAmount().doubleValue();
        
        // periodRate
        double j = getPeriodRate(i, compoundPeriodsPerYear); 
                //Math.pow( (1 + i/(compoundPeriodsPerYear*100.0)), (compoundPeriodsPerYear/12.0) ) - 1;
        // double j = Math.pow( (1 + i/200.0), (1.0/6.0) ); // Canadian simplified
        
        // periods per year (ie monthly payments)
        int n = 12;
        
        // amortization period in years
        double y = amortizationPeriod/12.0;
                
        double monthlyPayment = a*(j)/(1.0-Math.pow(j+1.0,-n*y));
        
        return monthlyPayment;
    }

    
    /**
     * Retrieve the interest rate for the compounding period based on the annual interest rate.
     * 
     * @param annualInterestRatePercent input annual interest rate as a percent (ie 8.25 for 8.25%)
     * @param compoundPeriodsPerYear 2 if compounding semi-annually, 12 if compounding monthly
     * @return interest rate as a decimal (ie .125 for 12.5%)
     */
    public static double getPeriodRate(double annualInterestRatePercent, int compoundPeriodsPerYear) {
        return Math.pow( 1 + annualInterestRatePercent / (compoundPeriodsPerYear * 100.0), compoundPeriodsPerYear / 12.0 ) - 1;
    }
    
    
}
