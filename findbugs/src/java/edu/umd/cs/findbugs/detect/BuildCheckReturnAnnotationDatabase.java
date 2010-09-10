/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.ClassElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.EnumElementValue;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnValueAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.AnnotationDatabase.Target;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

/**
 * Scan application classes for CheckReturnValue annotations.
 * 
 * @author David Hovemeyer
 * @author William Pugh
 */

public class BuildCheckReturnAnnotationDatabase extends AnnotationVisitor {

	private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

	private static final Map<String, AnnotationDatabase.Target> defaultKind = new HashMap<String, AnnotationDatabase.Target>();
	static {
		defaultKind.put("", AnnotationDatabase.Target.ANY);
		defaultKind.put("ForParameters", AnnotationDatabase.Target.PARAMETER);
		defaultKind.put("ForMethods", AnnotationDatabase.Target.METHOD);
		defaultKind.put("ForFields", AnnotationDatabase.Target.FIELD);

	}

	public BuildCheckReturnAnnotationDatabase() {

	}

	static String simpleClassName(@DottedClassName String className) {
		int i = className.lastIndexOf(".");
		if (i < 0)
			return className;
		return className.substring(i + 1);
	}

	@Override
	public void visitAnnotation(@DottedClassName String annotationClassName, Map<String, ElementValue> map, boolean runtimeVisible) {

		String annotationClassSimpleName = simpleClassName(annotationClassName);

		if (annotationClassSimpleName.startsWith("DefaultAnnotation")) {

			ElementValue v = map.get("value");
			if (!(v instanceof ClassElementValue))
				return;
			ClassElementValue value = (ClassElementValue) v;
			annotationClassSimpleName = annotationClassSimpleName.substring("DefaultAnnotation".length());

			Target annotationTarget = defaultKind.get(annotationClassSimpleName);
			if (annotationTarget != Target.METHOD)
				return;

			if (simpleClassName(value.getClassString()).equals("CheckReturnValue")) {
				CheckReturnValueAnnotation n = CheckReturnValueAnnotation.parse(getAnnotationParameterAsString(map, "priority"));
				if (n != null)
					AnalysisContext.currentAnalysisContext().getCheckReturnAnnotationDatabase().addDefaultAnnotation(
					        annotationTarget, getDottedClassName(), n);

			}

		}

		CheckReturnValueAnnotation n;

		if (annotationClassName.equals(javax.annotation.CheckReturnValue.class.getName())) {
			ElementValue v = map.get("when");
			if (v instanceof EnumElementValue) {
			EnumElementValue when = (EnumElementValue) v;
				String w = simpleClassName(when.getEnumValueString());
				if (w.equals("NEVER") || w.equals("UNKNOWN"))
					n = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE;
				else if (w.equals("MAYBE")) 
					n = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE;
				 else if (w.equals("ALWAYS")) 
					n = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH;
				else
					return;
			} else
				n = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;

		} else if (annotationClassName.equals(edu.umd.cs.findbugs.annotations.CheckReturnValue.class.getName())) {
			n = CheckReturnValueAnnotation.parse(getAnnotationParameterAsString(map, "priority"));
		} else if (annotationClassSimpleName.equals("CheckReturnValue")) {
			n = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;
		} else
			return;
		if (n == null) 
			return;
		if (visitingMethod())
			AnalysisContext.currentAnalysisContext().getCheckReturnAnnotationDatabase().addDirectAnnotation(
		        XFactory.createXMethod(this), n);
		else
			AnalysisContext.currentAnalysisContext().getCheckReturnAnnotationDatabase().addDefaultAnnotation(
					 Target.METHOD, getDottedClassName(), n);
			
	}

}
