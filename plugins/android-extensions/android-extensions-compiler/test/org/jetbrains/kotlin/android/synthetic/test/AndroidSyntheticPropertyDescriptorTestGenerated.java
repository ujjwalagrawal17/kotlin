/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.test;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/android-extensions/android-extensions-compiler/testData/descriptors")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class AndroidSyntheticPropertyDescriptorTestGenerated extends AbstractAndroidSyntheticPropertyDescriptorTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInDescriptors() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/android-extensions/android-extensions-compiler/testData/descriptors"), Pattern.compile("^([^\\.]+)$"), null, false);
    }

    @TestMetadata("escapedLayoutName")
    public void testEscapedLayoutName() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/escapedLayoutName/");
    }

    @TestMetadata("fqNameInAttr")
    public void testFqNameInAttr() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/fqNameInAttr/");
    }

    @TestMetadata("fqNameInTag")
    public void testFqNameInTag() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/fqNameInTag/");
    }

    @TestMetadata("layoutVariants")
    public void testLayoutVariants() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/layoutVariants/");
    }

    @TestMetadata("multiFile")
    public void testMultiFile() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/multiFile/");
    }

    @TestMetadata("noIds")
    public void testNoIds() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/noIds/");
    }

    @TestMetadata("nonLatinNames")
    public void testNonLatinNames() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/nonLatinNames/");
    }

    @TestMetadata("sameIds")
    public void testSameIds() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/sameIds/");
    }

    @TestMetadata("severalResDirs")
    public void testSeveralResDirs() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/severalResDirs/");
    }

    @TestMetadata("singleFile")
    public void testSingleFile() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/singleFile/");
    }

    @TestMetadata("specialTags")
    public void testSpecialTags() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/specialTags/");
    }

    @TestMetadata("supportSingleFile")
    public void testSupportSingleFile() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/supportSingleFile/");
    }

    @TestMetadata("supportSpecialTags")
    public void testSupportSpecialTags() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/supportSpecialTags/");
    }

    @TestMetadata("unresolvedFqName")
    public void testUnresolvedFqName() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/unresolvedFqName/");
    }

    @TestMetadata("unresolvedWidget")
    public void testUnresolvedWidget() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/unresolvedWidget/");
    }

    @TestMetadata("viewStub")
    public void testViewStub() throws Exception {
        runTest("plugins/android-extensions/android-extensions-compiler/testData/descriptors/viewStub/");
    }
}
