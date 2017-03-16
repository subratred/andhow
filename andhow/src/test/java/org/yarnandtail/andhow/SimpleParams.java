package org.yarnandtail.andhow;

import java.time.LocalDateTime;
import org.yarnandtail.andhow.property.FlagProp;
import org.yarnandtail.andhow.property.IntProp;
import org.yarnandtail.andhow.property.LngProp;
import org.yarnandtail.andhow.property.LocalDateTimeProp;
import org.yarnandtail.andhow.property.StrProp;

/**
 * Test set of params w/ one of each type.
 * 
 * Naming is [type]_[default value]
 * 
 * @author eeverman
 */
public interface SimpleParams extends PropertyGroup {
	
	//Strings
	StrProp STR_BOB = StrProp.builder().aliasIn("String_Bob").aliasInAndOut("Stringy.Bob").defaultValue("bob").build();
	StrProp STR_NULL = StrProp.builder().aliasInAndOut("String_Null").build();
	
	//Flags
	FlagProp FLAG_FALSE = FlagProp.builder().defaultValue(false).build();
	FlagProp FLAG_TRUE = FlagProp.builder().defaultValue(true).build();
	FlagProp FLAG_NULL = FlagProp.builder().build();
	
	//Integers
	IntProp INT_TEN = IntProp.builder().defaultValue(10).build();
	IntProp INT_NULL = IntProp.builder().build();
	
	//Long
	LngProp LNG_TEN = LngProp.builder().defaultValue(10L).build();
	LngProp LNG_NULL = LngProp.builder().build();
	
	//LocalDateTime
	LocalDateTimeProp LDT_2007_10_01 = LocalDateTimeProp.builder().defaultValue(LocalDateTime.parse("2007-10-01T00:00")).build();
	LocalDateTimeProp LDT_NULL = LocalDateTimeProp.builder().build();
}
