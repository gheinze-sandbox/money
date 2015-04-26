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
import java.time.LocalDate;
import java.util.Objects;

/**
 * A structure to hold the information for a payment which can represent a line
 * item in an amortization schedule
 * 
 * @author Glenn Heinze 
 */
public class ScheduledPayment {

    private int paymentNumber;
    private LocalDate paymentDate;
    private Money interest;
    private Money principal;
    private Money balance;


   
    @Override
    public String toString() {
        return "ScheduledPayment{" + "paymentNumber=" + paymentNumber + ", paymentDate=" + paymentDate + ", interest=" + interest + ", principal=" + principal + ", balance=" + balance + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.paymentNumber;
        hash = 29 * hash + Objects.hashCode(this.paymentDate);
        hash = 29 * hash + Objects.hashCode(this.interest);
        hash = 29 * hash + Objects.hashCode(this.principal);
        hash = 29 * hash + Objects.hashCode(this.balance);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScheduledPayment other = (ScheduledPayment) obj;
        if (this.paymentNumber != other.paymentNumber) {
            return false;
        }
        if (!Objects.equals(this.paymentDate, other.paymentDate)) {
            return false;
        }
        if (!Objects.equals(this.interest, other.interest)) {
            return false;
        }
        if (!Objects.equals(this.principal, other.principal)) {
            return false;
        }
        return Objects.equals(this.balance, other.balance);
    }
    
    
    public Money getPayment() {
        return getInterest().add( getPrincipal() );
    }

    public int getPaymentNumber() {
        return paymentNumber;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public Money getInterest() {
        return interest;
    }

    public Money getPrincipal() {
        return principal;
    }

    public Money getBalance() {
        return balance;
    }

    public void setPaymentNumber(int paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setInterest(Money interest) {
        this.interest = interest;
    }

    public void setPrincipal(Money principal) {
        this.principal = principal;
    }

    public void setBalance(Money balance) {
        this.balance = balance;
    }
    
    
}
