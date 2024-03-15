// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestSubClassMappingIsNotAutoGenerated extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MAIN_SOURCE_ID = "main.pure";
    private static final String TEST_SOURCE_ID = "test.pure";
    private static final String MAIN_SOURCE_CODE = "###Pure\n" +
            "import test::*;\n" +
            "\n" +
            "Class test::Person\n" +
            "{\n" +
            "   personId : Integer[1];\n" +
            "   personName : String[1];\n" +
            "   firmId : Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Firm\n" +
            "{\n" +
            "   firmId : Integer[1];\n" +
            "   firmName : String[1];\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database test::MainDatabase\n" +
            "(\n" +
            "   Table PersonTable(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)   \n" +
            "   Table FirmTable(firmId INT PRIMARY KEY, firmName VARCHAR(20))\n" +
            "   Join Person_Firm(PersonTable.firmId = FirmTable.firmId)\n" +
            ")\n" +
            "\n" +
            "###Mapping\n" +
            "import test::*;\n" +
            "\n" +
            "Mapping test::MainMapping\n" +
            "(  \n" +
            "   Person: Relational\n" +
            "   {\n" +
            "      scope([MainDatabase]PersonTable)\n" +
            "      (\n" +
            "        personId: personId,\n" +
            "        personName: personName,\n" +
            "        firmId: firmId\n" +
            "      )\n" +
            "   }\n" +
            "   \n" +
            "   Firm: Relational\n" +
            "   {\n" +
            "      scope([MainDatabase]FirmTable)\n" +
            "      (\n" +
            "        firmId: firmId,\n" +
            "        firmName: firmName\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    @Test
    public void testSubClassMappingNotExplicitlyCreated()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::MyPerson extends Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association test::MyPerson_Firm\n" +
                "{\n" +
                "   myPerson : test::MyPerson[*];\n" +
                "   firm : test::Firm[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::TestDatabase\n" +
                "(\n" +
                "   include test::MainDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(  \n" +
                "   include MainMapping\n" +
                "   \n" +
                "   MyPerson_Firm: Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         myPerson[test_Firm, test_MyPerson] : [TestDatabase]@Person_Firm,\n" +
                "         firm[test_MyPerson, test_Firm] : [TestDatabase]@Person_Firm   \n" +
                "      )       \n" +
                "   }  \n" +
                ")\n";
        this.verifyInValidSubClassMapping(testSourceCode);
    }

    @Test
    public void testSubClassMappingExplicitlyCreated()
    {
        String testSourceCode = "###Pure\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::MyPerson extends Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association test::MyPerson_Firm\n" +
                "{\n" +
                "   myPerson : test::MyPerson[*];\n" +
                "   firm : test::Firm[0..1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::TestDatabase\n" +
                "(\n" +
                "   include test::MainDatabase\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "\n" +
                "Mapping test::TestMapping\n" +
                "(  \n" +
                "   include MainMapping\n" +
                "   \n" +
                "   MyPerson extends [test_Person] : Relational\n" +
                "   {\n" +
                "\n" +
                "   }\n" +
                "   \n" +
                "   MyPerson_Firm: Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         myPerson[test_Firm, test_MyPerson] : [TestDatabase]@Person_Firm,\n" +
                "         firm[test_MyPerson, test_Firm] : [TestDatabase]@Person_Firm   \n" +
                "      )       \n" +
                "   }  \n" +
                ")\n";
        this.verifyValidSubClassMapping(testSourceCode);
    }

    private void verifyValidSubClassMapping(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compile()
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    private void verifyInValidSubClassMapping(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compileWithExpectedCompileFailure("Unable to find source class mapping (id:test_MyPerson) for property 'firm' in Association mapping 'MyPerson_Firm'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.",
                                TEST_SOURCE_ID, 32, 10)
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}