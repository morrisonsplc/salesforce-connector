/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

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
