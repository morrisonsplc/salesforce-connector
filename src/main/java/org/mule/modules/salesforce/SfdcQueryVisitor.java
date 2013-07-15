package org.mule.modules.salesforce;

import org.mule.common.query.DsqlQueryVisitor;
import org.mule.common.query.QueryVisitor;

/**
 *
 */

public class SfdcQueryVisitor extends DsqlQueryVisitor{

    @Override
    public org.mule.common.query.expression.OperatorVisitor operatorVisitor() {
        return new SfdcOperatorVisitor();
    }


}
