/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.set.overview;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Util {
    @FunctionalInterface
    public interface SupplierWithException<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
        T get() throws Exception;
    }

    public static <T> T fetch(final Object source, final String fieldName, final Class<T> resultClass) throws NoSuchFieldException {
        return fetch(source, source.getClass(), fieldName, resultClass);
    }

    public static <T> T fetch(final Object source, final Class<?> declaringClass, final String fieldName, final Class<T> resultClass) throws NoSuchFieldException {
        final Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return resultClass.cast(unchecked(() -> field.get(source)));
    }

    public static <T> T unchecked(final SupplierWithException<T> supplier) {
        try {
            return supplier.get();
        } catch (final Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public static String require(Properties props, String name) {
        String ret = (String) props.get(name);
        if (ret == null)
            throw new RuntimeException(name + " must be specified in processor.properties");

        return ret.trim();
    }
}
