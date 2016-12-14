/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.gef.common.reflect.Types;
import org.junit.Test;

import com.google.common.reflect.TypeToken;

public class TypesTests {

	private class ParameterizedSubType<T> extends ParameterizedType<T> {
	}

	private class ParameterizedSuperType<T> extends Object {
	}

	private class ParameterizedType<T> extends ParameterizedSuperType<T> {
	}

	private class ParameterType1 extends Object {
	}

	private class ParameterType2 extends Object {
	}

	@SuppressWarnings("serial")
	@Test
	public void serializeAndDeserializeTypeToken() {
		TypeToken<ParameterizedType<ParameterType1>> typeToken = new TypeToken<ParameterizedType<ParameterType1>>() {
		};
		String typeTokenString = Types.serialize(typeToken);
		TypeToken<?> deserializedTypeToken = Types.deserialize(typeTokenString);
		assertEquals(typeToken, deserializedTypeToken);

		TypeToken<ParameterizedType<ParameterType2>> typeToken2 = new TypeToken<ParameterizedType<ParameterType2>>() {
		};
		String typeTokenString2 = Types.serialize(typeToken2);
		TypeToken<?> deserializedTypeToken2 = Types
				.deserialize(typeTokenString2);
		assertEquals(typeToken2, deserializedTypeToken2);

		assertNotEquals(typeToken, typeToken2);
		assertNotEquals(deserializedTypeToken, deserializedTypeToken2);
	}

}
