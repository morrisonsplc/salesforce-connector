package org.mule.modules.salesforce;

import org.mule.common.query.DefaultOperatorVisitor;

/**
 */

public class SfdcOperatorVisitor extends DefaultOperatorVisitor {

    @Override
    public java.lang.String notEqualsOperator() {
        return " != ";
    }

}
