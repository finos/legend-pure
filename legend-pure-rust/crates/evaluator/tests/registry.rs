// Copyright 2026 Goldman Sachs
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

//! The native registry holds exactly the natives Step 1 fished, under
//! every signature the platform declares for them. Fishing a native
//! without listing it here is a review failure (plan rule 4: the
//! registry's contents must be explainable by a test).

use legend_pure_evaluator::native::NativeRegistry;

const FISHED: &[&str] = &[
    // arithmetic: one implementation per operator, four signatures each
    "plus_Integer_MANY__Integer_1_",
    "plus_Float_MANY__Float_1_",
    "plus_Decimal_MANY__Decimal_1_",
    "plus_Number_MANY__Number_1_",
    "minus_Integer_MANY__Integer_1_",
    "minus_Float_MANY__Float_1_",
    "minus_Decimal_MANY__Decimal_1_",
    "minus_Number_MANY__Number_1_",
    "times_Integer_MANY__Integer_1_",
    "times_Float_MANY__Float_1_",
    "times_Decimal_MANY__Decimal_1_",
    "times_Number_MANY__Number_1_",
    // boolean
    "and_Boolean_1__Boolean_1__Boolean_1_",
    "or_Boolean_1__Boolean_1__Boolean_1_",
    "not_Boolean_1__Boolean_1_",
    // comparison
    "eq_Any_1__Any_1__Boolean_1_",
    "equal_Any_MANY__Any_MANY__Boolean_1_",
    // lang
    "letFunction_String_1__T_m__T_m_",
    "if_Boolean_1__Function_1__Function_1__T_m_",
    "new_Class_1__String_1__Any_MANY__T_1_",
    "new_Class_1__String_1__T_1_",
    "new_Class_1__String_1__KeyExpression_MANY__T_1_",
    "copy_T_1__KeyExpression_MANY__T_1_",
    "copy_T_1__String_1__T_1_",
    "copy_T_1__String_1__KeyExpression_MANY__T_1_",
    // testing
    "assert_Boolean_1__Function_1__Boolean_1_",
];

#[test]
fn standard_registry_holds_exactly_the_fished_natives() {
    let registry = NativeRegistry::standard();
    let mut actual: Vec<&str> = registry.keys().map(|k| k.as_str()).collect();
    actual.sort_unstable();
    let mut expected: Vec<&str> = FISHED.to_vec();
    expected.sort_unstable();
    assert_eq!(actual, expected);
    assert_eq!(registry.len(), FISHED.len());
    for key in FISHED {
        assert!(registry.get(key).is_some(), "{key} must dispatch");
    }
}
