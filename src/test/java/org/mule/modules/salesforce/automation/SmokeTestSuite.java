/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce.automation;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.mule.modules.salesforce.automation.testcases.*;

@RunWith(Categories.class)
@IncludeCategory(SmokeTests.class)

@SuiteClasses({
	
	ConvertLeadTestCases.class,
	CreateSingleTestCases.class,
	CreateTestCases.class,
	DeleteTestCases.class,
	DescribeGlobalTestCases.class,
	DescribeSObjectTestCases.class,
	EmptyRecycleBinTestCases.class,
	GetDeletedRangeTestCases.class,
	GetDeletedTestCases.class,
	GetUpdatedObjectsTestCases.class,
	GetUpdatedRangeTestCases.class,
	GetUpdatedTestCases.class,
	GetUserInfoTestCases.class,
	PaginatedQueryTestCases.class,
	QueryAllTestCases.class,
	QuerySingleTestCases.class,
	QueryTestCases.class,
	RetrieveTestCases.class,
	SearchTestCases.class,
	UpdateSingleTestCases.class,
	UpdateTestCases.class,
	UpsertTestCases.class,
	
		})

public class SmokeTestSuite {
	
}