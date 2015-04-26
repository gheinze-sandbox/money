package com.accounted4.money.loan;

/**
 *
 * @author glenn
 */
public enum CompoundingPeriod {

     Monthly(12)
    ,SemiAnnually(2)
    ,Annually(1)
    ;
    
    private final int compoundingPeriodsPerYear;

    private CompoundingPeriod(int compoundingPeriods) {
        this.compoundingPeriodsPerYear = compoundingPeriods;
    }

    
    public int getCompoundingPeriodsPerYear() {
        return compoundingPeriodsPerYear;
    }
    
}
