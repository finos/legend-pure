package org.finos.legend.pure.runtime.java.extension.functions.interpreted;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.ConstructorForPairList;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.GetIfAbsentPutWithKey;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.GetMapStats;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.Keys;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.KeyValues;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.Put;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.PutAllMaps;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.PutAllPairs;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.ReplaceAll;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.Values;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.ReplaceTreeNode;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.cipher.Cipher;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.cipher.Decipher;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Exists;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.ForAll;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.IndexOf;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Repeat;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Last;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Take;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Drop;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Slice;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Zip;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.RemoveAllOptimized;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Reverse;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Get;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.GroupBy;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.AdjustDate;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DateDiff;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DatePart;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DayOfMonth;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DayOfWeekNumber;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasDay;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasHour;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasMinute;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasMonth;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasSecond;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasSubsecond;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.HasSubsecondWithAtLeastPrecision;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Hour;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Minute;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.MonthNumber;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.NewDate;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Now;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Second;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Today;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.WeekOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Year;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.hash.Hash;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.io.ReadFile;

import org.finos.legend.pure.runtime.java.interpreted.natives.basics.lang.Match;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.lang.RawEvalProperty;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.lang.RemoveOverride;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ArcCosine;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ArcSine;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ArcTangent;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ArcTangent2;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Ceiling;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Cosine;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Exp;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Floor;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Log;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Mod;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Power;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Rem;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Round;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.RoundWithScale;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Sine;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Sqrt;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.StdDev;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.Tangent;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ToDecimal;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math.ToFloat;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.CanReactivateDynamically;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.CompileValueSpecification;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.Deactivate;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.EnumName;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.EnumValues;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.FunctionDescriptorToId;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.Generalizations;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.IsSourceReadOnly;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.IsValidFunctionDescriptor;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewAssociation;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewClass;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewEnumeration;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewLambdaFunction;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewProperty;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.meta.NewUnit;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.OpenVariableValues;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.Reactivate;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.Stereotype;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.SubTypeOf;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.Tag;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.runtime.Guid;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.runtime.IsOptionSet;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Chunk;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Contains;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.DecodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.EncodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.EndsWith;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.IndexOfString;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Matches;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ParsePrimitiveBoolean;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ParsePrimitiveDate;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ParsePrimitiveDecimal;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ParsePrimitiveFloat;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ParsePrimitiveInteger;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ToInteger;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ToLower;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ToUpper;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Trim;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.tracing.TraceSpan;

import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.AlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.LegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.Profile;


public class FunctionExtensionInterpreted extends BaseInterpretedExtension
{
    public FunctionExtensionInterpreted()
    {
        super(Lists.mutable.with(
                //Cipher
                Tuples.pair("encrypt_String_1__String_1__String_1_", Cipher::new),
                Tuples.pair("encrypt_Number_1__String_1__String_1_", Cipher::new),
                Tuples.pair("encrypt_Boolean_1__String_1__String_1_", Cipher::new),
                Tuples.pair("decrypt_String_1__String_1__String_1_", Decipher::new),

                //Collection
                Tuples.pair("drop_T_MANY__Integer_1__T_MANY_", Drop::new),
                Tuples.pair("exists_T_MANY__Function_1__Boolean_1_", Exists::new),
                Tuples.pair("forAll_T_MANY__Function_1__Boolean_1_", ForAll::new),
                Tuples.pair("get_T_MANY__String_1__T_$0_1$_", Get::new),
                Tuples.pair("groupBy_X_MANY__Function_1__Map_1_", GroupBy::new),
                Tuples.pair("indexOf_T_MANY__T_1__Integer_1_", IndexOf::new),
                Tuples.pair("last_T_MANY__T_$0_1$_", Last::new),
                Tuples.pair("removeAllOptimized_T_MANY__T_MANY__T_MANY_", RemoveAllOptimized::new),
                Tuples.pair("repeat_T_1__Integer_1__T_MANY_", Repeat::new),
                Tuples.pair("reverse_T_m__T_m_", Reverse::new),
                Tuples.pair("slice_T_MANY__Integer_1__Integer_1__T_MANY_", Slice::new),
                Tuples.pair("take_T_MANY__Integer_1__T_MANY_", Take::new),
                Tuples.pair("zip_T_MANY__U_MANY__Pair_MANY_", Zip::new),

                //Date
                Tuples.pair("adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", AdjustDate::new),
                Tuples.pair("dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_", DateDiff::new),
                Tuples.pair("datePart_Date_1__Date_1_", DatePart::new),
                Tuples.pair("dayOfMonth_Date_1__Integer_1_", DayOfMonth::new),
                Tuples.pair("dayOfWeekNumber_Date_1__Integer_1_", DayOfWeekNumber::new),
                Tuples.pair("hasDay_Date_1__Boolean_1_", HasDay::new),
                Tuples.pair("hasHour_Date_1__Boolean_1_", HasHour::new),
                Tuples.pair("hasMinute_Date_1__Boolean_1_", HasMinute::new),
                Tuples.pair("hasMonth_Date_1__Boolean_1_", HasMonth::new),
                Tuples.pair("hasSecond_Date_1__Boolean_1_", HasSecond::new),
                Tuples.pair("hasSubsecond_Date_1__Boolean_1_", HasSubsecond::new),
                Tuples.pair("hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_", HasSubsecondWithAtLeastPrecision::new),
                Tuples.pair("hour_Date_1__Integer_1_", Hour::new),
                Tuples.pair("minute_Date_1__Integer_1_", Minute::new),
                Tuples.pair("monthNumber_Date_1__Integer_1_", MonthNumber::new),
                Tuples.pair("date_Integer_1__Date_1_", NewDate::new),
                Tuples.pair("date_Integer_1__Integer_1__Date_1_", NewDate::new),
                Tuples.pair("date_Integer_1__Integer_1__Integer_1__StrictDate_1_", NewDate::new),
                Tuples.pair("date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", NewDate::new),
                Tuples.pair("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", NewDate::new),
                Tuples.pair("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_", NewDate::new),
                Tuples.pair("now__DateTime_1_", Now::new),
                Tuples.pair("second_Date_1__Integer_1_", Second::new),
                Tuples.pair("today__StrictDate_1_", Today::new),
                Tuples.pair("weekOfYear_Date_1__Integer_1_", WeekOfYear::new),
                Tuples.pair("year_Date_1__Integer_1_", Year::new),

                //Hash
                Tuples.pair("hash_String_1__HashType_1__String_1_", Hash::new),

                //IO
                Tuples.pair("executeHTTPRaw_URL_1__HTTPMethod_1__String_$0_1$__String_$0_1$__HTTPResponse_1_", Http::new),
                Tuples.pair("readFile_String_1__String_$0_1$_", ReadFile::new),

                //Lang
                Tuples.pair("match_Any_MANY__Function_$1_MANY$__P_o__T_m_", Match::new),
                Tuples.pair("mutateAdd_T_1__String_1__Any_MANY__T_1_", MutateAdd::new),
                Tuples.pair("rawEvalProperty_Property_1__Any_1__V_m_", RawEvalProperty::new),
                Tuples.pair("removeOverride_T_1__T_1_", RemoveOverride::new),

                //Math
                Tuples.pair("acos_Number_1__Float_1_", ArcCosine::new),
                Tuples.pair("asin_Number_1__Float_1_", ArcSine::new),
                Tuples.pair("atan_Number_1__Float_1_", ArcTangent::new),
                Tuples.pair("atan2_Number_1__Number_1__Float_1_", ArcTangent2::new),
                Tuples.pair("ceiling_Number_1__Integer_1_", Ceiling::new),
                Tuples.pair("cos_Number_1__Float_1_", Cosine::new),
                Tuples.pair("exp_Number_1__Float_1_", Exp::new),
                Tuples.pair("floor_Number_1__Integer_1_", Floor::new),
                Tuples.pair("log_Number_1__Float_1_", Log::new),
                Tuples.pair("mod_Integer_1__Integer_1__Integer_1_", Mod::new),
                Tuples.pair("pow_Number_1__Number_1__Number_1_", Power::new),
                Tuples.pair("rem_Number_1__Number_1__Number_1_", Rem::new),
                Tuples.pair("round_Number_1__Integer_1_", Round::new),
                Tuples.pair("round_Decimal_1__Integer_1__Decimal_1_", RoundWithScale::new),
                Tuples.pair("round_Float_1__Integer_1__Float_1_", RoundWithScale::new),
                Tuples.pair("sin_Number_1__Float_1_", Sine::new),
                Tuples.pair("sqrt_Number_1__Float_1_", Sqrt::new),
                Tuples.pair("stdDev_Number_$1_MANY$__Boolean_1__Number_1_", StdDev::new),
                Tuples.pair("tan_Number_1__Float_1_", Tangent::new),
                Tuples.pair("toDecimal_Number_1__Decimal_1_", ToDecimal::new),
                Tuples.pair("toFloat_Number_1__Float_1_", ToFloat::new),


                //Meta
                Tuples.pair("canReactivateDynamically_ValueSpecification_1__Boolean_1_", CanReactivateDynamically::new),
                Tuples.pair("compileValueSpecification_String_m__CompilationResult_m_", CompileValueSpecification::new),
                Tuples.pair("deactivate_Any_MANY__ValueSpecification_1_", Deactivate::new),
                Tuples.pair("enumName_Enumeration_1__String_1_", EnumName::new),
                Tuples.pair("enumValues_Enumeration_1__T_MANY_", EnumValues::new),
                Tuples.pair("functionDescriptorToId_String_1__String_1_", FunctionDescriptorToId::new),
                Tuples.pair("generalizations_Type_1__Type_$1_MANY$_", Generalizations::new),
                Tuples.pair("isSourceReadOnly_String_1__Boolean_1_", IsSourceReadOnly::new),
                Tuples.pair("isValidFunctionDescriptor_String_1__Boolean_1_", IsValidFunctionDescriptor::new),
                Tuples.pair("newAssociation_String_1__Property_1__Property_1__Association_1_", NewAssociation::new),
                Tuples.pair("newClass_String_1__Class_1_", NewClass::new),
                Tuples.pair("newEnumeration_String_1__String_MANY__Enumeration_1_", NewEnumeration::new),
                Tuples.pair("newLambdaFunction_FunctionType_1__LambdaFunction_1_", NewLambdaFunction::new),
                Tuples.pair("newProperty_String_1__GenericType_1__GenericType_1__Multiplicity_1__Property_1_", NewProperty::new),
                Tuples.pair("openVariableValues_Function_1__Map_1_", OpenVariableValues::new),
                Tuples.pair("reactivate_ValueSpecification_1__Map_1__Any_MANY_", Reactivate::new),
                Tuples.pair("sourceInformation_Any_1__SourceInformation_$0_1$_", SourceInformation::new),
                Tuples.pair("stereotype_Profile_1__String_1__Stereotype_1_", Stereotype::new),
                Tuples.pair("subTypeOf_Type_1__Type_1__Boolean_1_", SubTypeOf::new),
                Tuples.pair("tag_Profile_1__String_1__Tag_1_", Tag::new),


                //Runtime
                Tuples.pair("currentUserId__String_1_", CurrentUserId::new),
                Tuples.pair("generateGuid__String_1_", Guid::new),
                Tuples.pair("isOptionSet_String_1__Boolean_1_", IsOptionSet::new),


                //String
                Tuples.pair("chunk_String_1__Integer_1__String_MANY_", Chunk::new),
                Tuples.pair("contains_String_1__String_1__Boolean_1_", Contains::new),
                Tuples.pair("encodeBase64_String_1__String_1_", EncodeBase64::new),
                Tuples.pair("decodeBase64_String_1__String_1_", DecodeBase64::new),
                Tuples.pair("endsWith_String_1__String_1__Boolean_1_", EndsWith::new),
                Tuples.pair("indexOf_String_1__String_1__Integer_1_", IndexOfString::new),
                Tuples.pair("indexOf_String_1__String_1__Integer_1__Integer_1_", IndexOfString::new),
                Tuples.pair("matches_String_1__String_1__Boolean_1_", Matches::new),
                Tuples.pair("parseBoolean_String_1__Boolean_1_", ParsePrimitiveBoolean::new),
                Tuples.pair("parseDate_String_1__Date_1_", ParsePrimitiveDate::new),
                Tuples.pair("parseFloat_String_1__Float_1_", ParsePrimitiveFloat::new),
                Tuples.pair("parseDecimal_String_1__Decimal_1_", ParsePrimitiveDecimal::new),
                Tuples.pair("parseInteger_String_1__Integer_1_", ParsePrimitiveInteger::new),
                Tuples.pair("toInteger_String_1__Integer_1_", ToInteger::new),
                Tuples.pair("toLower_String_1__String_1_", ToLower::new),
                Tuples.pair("toUpper_String_1__String_1_", ToUpper::new),
                Tuples.pair("trim_String_1__String_1_", Trim::new),


                //Tracing
                Tuples.pair("traceSpan_Function_1__String_1__V_m_", TraceSpan::new),
                Tuples.pair("traceSpan_Function_1__String_1__Function_1__V_m_", TraceSpan::new),
                Tuples.pair("traceSpan_Function_1__String_1__Function_1__Boolean_1__V_m_", TraceSpan::new),

                //LegendTests
                Tuples.pair("mayExecuteLegendTest_Function_1__Function_1__X_k_", LegendTest::new),
                Tuples.pair("mayExecuteAlloyTest_Function_1__Function_1__X_k_", AlloyTest::new),

                //Tools
                Tuples.pair("profile_T_m__Boolean_1__ProfileResult_1_", Profile::new),


                //Anonymous Collections
                Tuples.pair("newMap_Pair_MANY__Map_1_", ConstructorForPairList::new),
                Tuples.pair("newMap_Pair_MANY__Property_MANY__Map_1_", ConstructorForPairList::new),
                Tuples.pair("get_Map_1__U_1__V_$0_1$_", org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map.Get::new),
                Tuples.pair("getIfAbsentPutWithKey_Map_1__U_1__Function_1__V_$0_1$_", GetIfAbsentPutWithKey::new),
                Tuples.pair("getMapStats_Map_1__MapStats_$0_1$_", GetMapStats::new),
                Tuples.pair("keys_Map_1__U_MANY_", Keys::new),
                Tuples.pair("keyValues_Map_1__Pair_MANY_", KeyValues::new),
                Tuples.pair("put_Map_1__U_1__V_1__Map_1_", Put::new),
                Tuples.pair("putAll_Map_1__Map_1__Map_1_", PutAllMaps::new),
                Tuples.pair("putAll_Map_1__Pair_MANY__Map_1_", PutAllPairs::new),
                Tuples.pair("replaceAll_Map_1__Pair_MANY__Map_1_", ReplaceAll::new),
                Tuples.pair("values_Map_1__V_MANY_", Values::new),

                Tuples.pair("replaceTreeNode_TreeNode_1__TreeNode_1__TreeNode_1__TreeNode_1_", ReplaceTreeNode::new)

        ));
    }

    public static FunctionExtensionInterpreted extension()
    {
        return new FunctionExtensionInterpreted();
    }
}

