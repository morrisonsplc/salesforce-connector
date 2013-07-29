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
	
	AbortJobTestCases.class,
	CreateTestCases.class,
	GetUpdatedTestCases.class,
	RetrieveTestCases.class,
	DeleteTestCases.class,
	GetUserInfoTestCases.class,
	CloseJobTestCases.class,
	DescribeGlobalTestCases.class,
	HardDeleteBulkTestCases.class,
	SearchTestCases.class,
	ConvertLeadTestCases.class,
	DescribeSObjectTestCases.class,
	PaginatedQueryTestCases.class,
	EmptyRecycleBinTestCases.class,
	QueryAllTestCases.class,
	UpdateBulkTestCases.class,
	CreateBatchTestCases.class,
	GetDeletedRangeTestCases.class,
	QueryResultStreamTestCases.class,
	UpdateSingleTestCases.class,
	CreateBulkTestCases.class,
	GetDeletedTestCases.class,
	QuerySingleTestCases.class,
	UpdateTestCases.class,
	CreateJobTestCases.class,
	GetUpdatedObjectsTestCases.class,
	QueryTestCases.class,
	UpsertBulkTestCases.class,
	CreateSingleTestCases.class,
	GetUpdatedRangeTestCases.class,
	UpsertTestCases.class
	
		})

public class SmokeTestSuite {
	
}