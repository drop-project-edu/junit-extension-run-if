/*-
 * ========================LICENSE_START=================================
 * "Run if" extension for JUnit
 * %%
 * Copyright (C) 2021 Pedro Alves
 * %%
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
 * =========================LICENSE_END==================================
 */
package pt.ulusofona.deisi;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(ConditionalRunner.class)
public class TestClass1 {

    @Test
    public void test01() {
        assertTrue(true);
    }

    @Test
    @RunIf({"test01"})
    public void test02() {
        assertTrue(true);
    }

    @Test
    @RunIf({"test01", "test02"})
    public void test03() {
        assertTrue(true);
    }
}
