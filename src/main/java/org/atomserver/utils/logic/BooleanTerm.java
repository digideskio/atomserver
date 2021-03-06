/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
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
 */


package org.atomserver.utils.logic;

import java.util.Set;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BooleanTerm<T> implements BooleanExpression<T> {
    private final String name;
    private final T value;

    public BooleanTerm(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public ExpressionType getType() {
        return ExpressionType.TERM;
    }

    public void buildTermSet(Set<BooleanTerm<? extends T>> terms) {
        terms.add(this);
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("[ TERM:: ");
        buff.append(" name= ").append(name);
        buff.append(" value= ").append(value);
        buff.append(" ]");
        return buff.toString();
    }

}
