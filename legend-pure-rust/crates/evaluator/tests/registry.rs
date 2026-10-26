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

//! The native registry holds exactly the natives fished so far, under
//! every signature the platform declares for them.

use legend_pure_evaluator::native::NativeRegistry;

const FISHED: &[&str] = &[
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
    "divide_Number_1__Number_1__Float_1_",
    "divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_",
    "abs_Integer_1__Integer_1_",
    "abs_Float_1__Float_1_",
    "abs_Decimal_1__Decimal_1_",
    "abs_Number_1__Number_1_",
    "mod_Integer_1__Integer_1__Integer_1_",
    "rem_Number_1__Number_1__Number_1_",
    "and_Boolean_1__Boolean_1__Boolean_1_",
    "or_Boolean_1__Boolean_1__Boolean_1_",
    "not_Boolean_1__Boolean_1_",
    "fold_T_MANY__Function_1__V_m__V_m_",
    "take_T_MANY__Integer_1__T_MANY_",
    "drop_T_MANY__Integer_1__T_MANY_",
    "zip_T_MANY__U_MANY__Pair_MANY_",
    "first_T_MANY__T_$0_1$_",
    "removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_",
    "sort_T_m__Function_$0_1$__Function_$0_1$__T_m_",
    "size_Any_MANY__Integer_1_",
    "range_Integer_1__Integer_1__Integer_1__Integer_MANY_",
    "map_T_MANY__Function_1__V_MANY_",
    "map_T_m__Function_1__V_m_",
    "map_T_$0_1$__Function_1__V_$0_1$_",
    "slice_T_MANY__Integer_1__Integer_1__T_MANY_",
    "find_T_MANY__Function_1__T_$0_1$_",
    "at_T_MANY__Integer_1__T_1_",
    "exists_T_MANY__Function_1__Boolean_1_",
    "indexOf_T_MANY__T_1__Integer_1_",
    "tail_T_MANY__T_MANY_",
    "toOne_T_MANY__T_1_",
    "toOne_T_MANY__String_1__T_1_",
    "filter_T_MANY__Function_1__T_MANY_",
    "concatenate_T_MANY__T_MANY__T_MANY_",
    "forAll_T_MANY__Function_1__Boolean_1_",
    "init_T_MANY__T_MANY_",
    "last_T_MANY__T_$0_1$_",
    "put_Map_1__U_1__V_1__Map_1_",
    "putAll_Map_1__Pair_MANY__Map_1_",
    "putAll_Map_1__Map_1__Map_1_",
    "isEmpty_Any_MANY__Boolean_1_",
    "add_T_MANY__T_1__T_$1_MANY$_",
    "add_T_MANY__Integer_1__T_1__T_$1_MANY$_",
    "reverse_T_m__T_m_",
    "get_Map_1__U_1__V_$0_1$_",
    "keys_Map_1__U_MANY_",
    "values_Map_1__V_MANY_",
    "newMap_Pair_MANY__Map_1_",
    "newMap_Pair_MANY__Property_MANY__Map_1_",
    "eq_Any_1__Any_1__Boolean_1_",
    "equal_Any_MANY__Any_MANY__Boolean_1_",
    "lessThan_Number_1__Number_1__Boolean_1_",
    "lessThanEqual_Number_1__Number_1__Boolean_1_",
    "compare_T_1__T_1__Integer_1_",
    "year_Date_1__Integer_1_",
    "monthNumber_Date_1__Integer_1_",
    "dayOfMonth_Date_1__Integer_1_",
    "hour_Date_1__Integer_1_",
    "minute_Date_1__Integer_1_",
    "second_Date_1__Integer_1_",
    "datePart_Date_1__Date_1_",
    "dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_",
    "adjust_Date_1__Integer_1__DurationUnit_1__Date_1_",
    "hasMonth_Date_1__Boolean_1_",
    "hasDay_Date_1__Boolean_1_",
    "hasHour_Date_1__Boolean_1_",
    "hasMinute_Date_1__Boolean_1_",
    "hasSecond_Date_1__Boolean_1_",
    "hasSubsecond_Date_1__Boolean_1_",
    "hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_",
    "parseDate_String_1__Date_1_",
    "date_Integer_1__Date_1_",
    "date_Integer_1__Integer_1__Date_1_",
    "date_Integer_1__Integer_1__Integer_1__StrictDate_1_",
    "date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
    "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
    "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_",
    "letFunction_String_1__T_m__T_m_",
    "if_Boolean_1__Function_1__Function_1__T_m_",
    "eval_Function_1__V_m_",
    "eval_Function_1__T_n__V_m_",
    "eval_Function_1__T_n__U_p__V_m_",
    "eval_Function_1__T_n__U_p__W_q__V_m_",
    "eval_Function_1__T_n__U_p__W_q__X_r__V_m_",
    "eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_",
    "eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_",
    "eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_",
    "new_Class_1__String_1__T_1_",
    "new_Class_1__String_1__KeyExpression_MANY__T_1_",
    "getAll_Class_1__T_MANY_",
    "copy_T_1__String_1__T_1_",
    "copy_T_1__String_1__KeyExpression_MANY__T_1_",
    "floor_Number_1__Integer_1_",
    "ceiling_Number_1__Integer_1_",
    "round_Number_1__Integer_1_",
    "round_Decimal_1__Integer_1__Decimal_1_",
    "round_Float_1__Integer_1__Float_1_",
    "sign_Number_1__Integer_1_",
    "sqrt_Number_1__Float_1_",
    "cbrt_Number_1__Float_1_",
    "exp_Number_1__Float_1_",
    "log_Number_1__Float_1_",
    "log10_Number_1__Float_1_",
    "pow_Number_1__Number_1__Number_1_",
    "sin_Number_1__Float_1_",
    "cos_Number_1__Float_1_",
    "tan_Number_1__Float_1_",
    "cot_Number_1__Float_1_",
    "asin_Number_1__Float_1_",
    "acos_Number_1__Float_1_",
    "atan_Number_1__Float_1_",
    "atan2_Number_1__Number_1__Float_1_",
    "toFloat_Number_1__Float_1_",
    "toDecimal_Number_1__Decimal_1_",
    "parseInteger_String_1__Integer_1_",
    "parseFloat_String_1__Float_1_",
    "parseBoolean_String_1__Boolean_1_",
    "match_Any_MANY__Function_$1_MANY$__T_m_",
    "match_Any_MANY__Function_$1_MANY$__P_o__T_m_",
    "extractEnumValue_Enumeration_1__String_1__T_1_",
    "cast_Any_m__T_1__T_m_",
    "genericType_Any_MANY__GenericType_1_",
    "deactivate_Any_MANY__ValueSpecification_1_",
    "toString_Any_1__String_1_",
    "format_String_1__Any_MANY__String_1_",
    "joinStrings_String_MANY__String_1__String_1__String_1__String_1_",
    "indexOf_String_1__String_1__Integer_1_",
    "indexOf_String_1__String_1__Integer_1__Integer_1_",
    "lastIndexOf_String_1__String_1__Integer_1__Integer_1_",
    "startsWith_String_1__String_1__Boolean_1_",
    "parseDecimal_String_1__Decimal_1_",
    "parseDecimal_String_1__Integer_1__Integer_1__Decimal_1_",
    "contains_String_1__String_1__Boolean_1_",
    "endsWith_String_1__String_1__Boolean_1_",
    "length_String_1__Integer_1_",
    "split_String_1__String_1__String_MANY_",
    "substring_String_1__Integer_1__Integer_1__String_1_",
    "substring_String_1__Integer_1__String_1_",
    "replace_String_1__String_1__String_1__String_1_",
    "reverseString_String_1__String_1_",
    "toLower_String_1__String_1_",
    "toUpper_String_1__String_1_",
    "ltrim_String_1__String_1_",
    "rtrim_String_1__String_1_",
    "trim_String_1__String_1_",
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

#[derive(Debug)]
struct Constant(i64);

impl legend_pure_evaluator::native::NativeFunction for Constant {
    fn execute(
        &self,
        _args: &[legend_pure_metamodel::types::ValueSpec],
        _ctx: &mut dyn legend_pure_evaluator::native::EvalContextTrait,
    ) -> Result<legend_pure_evaluator::native::Evaluated, legend_pure_evaluator::error::PureException>
    {
        Ok(legend_pure_evaluator::native::Evaluated::new(
            legend_pure_evaluator::value::Value::Integer(self.0),
        ))
    }
}

#[test]
fn registering_a_name_again_replaces_it_at_the_same_position() {
    let mut registry = NativeRegistry::new();
    registry.register("a", Constant(1));
    registry.register("b", Constant(2));
    let position = registry.position("a").expect("registered");
    registry.register("a", Constant(3));
    assert_eq!(registry.len(), 2);
    assert_eq!(registry.position("a"), Some(position));
    assert_eq!(format!("{:?}", registry.at(position)), "Constant(3)");
    assert!(registry.position("c").is_none());
}

/// How many registry keys take their arguments each way: eager (the body
/// executor forces the arguments and calls `execute_values`), a control
/// form, or lazy. A native moves to eager only by declaring it.
#[test]
fn registry_keys_by_argument_mode() {
    use legend_pure_evaluator::native::ArgMode;
    let registry = NativeRegistry::standard();
    let (mut eager, mut control, mut lazy) = (0, 0, 0);
    for key in registry.keys() {
        match registry.get(key).expect("registered").arg_mode() {
            ArgMode::Eager(_) => eager += 1,
            ArgMode::Control(_) => control += 1,
            ArgMode::Lazy => lazy += 1,
        }
    }
    assert_eq!((eager, control, lazy), (69, 4, 84), "eager, control, lazy");
}

/// The keys whose native declares an intrinsic, and which. A native's
/// call runs inline only by its declaring one, and only an eager native
/// declares one.
#[test]
fn registry_keys_declaring_an_intrinsic() {
    use legend_pure_evaluator::native::{ArgMode, Intrinsic};
    let registry = NativeRegistry::standard();
    let mut declared: Vec<(&str, Intrinsic)> = registry
        .keys()
        .filter_map(|key| {
            let native = registry.get(key).expect("registered");
            native.intrinsic().map(|intrinsic| {
                assert!(
                    matches!(native.arg_mode(), ArgMode::Eager(_)),
                    "{key} declares an intrinsic and is not eager"
                );
                (key.as_str(), intrinsic)
            })
        })
        .collect();
    declared.sort_unstable_by_key(|(key, _)| *key);
    assert_eq!(
        declared,
        [
            ("eq_Any_1__Any_1__Boolean_1_", Intrinsic::Equal),
            ("equal_Any_MANY__Any_MANY__Boolean_1_", Intrinsic::Equal),
            ("isEmpty_Any_MANY__Boolean_1_", Intrinsic::IsEmpty),
            (
                "lessThanEqual_Number_1__Number_1__Boolean_1_",
                Intrinsic::LessThanEqual
            ),
            (
                "lessThan_Number_1__Number_1__Boolean_1_",
                Intrinsic::LessThan
            ),
            ("not_Boolean_1__Boolean_1_", Intrinsic::Not),
            ("size_Any_MANY__Integer_1_", Intrinsic::Size),
        ]
    );
}
