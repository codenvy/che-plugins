/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.compiler.util;

import java.util.Arrays;
import java.util.Vector;

public final class ObjectVector {

    static int INITIAL_SIZE = 10;
//
//    public int size;
//
//    int maxSize;
//
//    Object[] elements;

    private final Vector<Object> objects;

    public ObjectVector() {
        this(INITIAL_SIZE);
    }

    public ObjectVector(int initialSize) {
//        this.maxSize = initialSize > 0 ? initialSize : INITIAL_SIZE;
//        this.size = 0;
//        this.elements = new Object[this.maxSize];
        objects = new Vector<>(initialSize);
    }

    public void add(Object newElement) {

//        if (this.size == this.maxSize) // knows that size starts <= maxSize
//            System.arraycopy(this.elements, 0, (this.elements = new Object[this.maxSize *= 2]), 0, this.size);
//        this.elements[this.size++] = newElement;
        objects.add(newElement);
    }

    public void addAll(Object[] newElements) {

//        if (this.size + newElements.length >= this.maxSize) {
//            this.maxSize = this.size + newElements.length; // assume no more elements will be added
//            System.arraycopy(this.elements, 0, (this.elements = new Object[this.maxSize]), 0, this.size);
//        }
//        System.arraycopy(newElements, 0, this.elements, this.size, newElements.length);
//        this.size += newElements.length;
        objects.addAll(Arrays.asList(newElements));
    }

    public void addAll(ObjectVector newVector) {

//        if (this.size + newVector.size >= this.maxSize) {
//            this.maxSize = this.size + newVector.size; // assume no more elements will be added
//            System.arraycopy(this.elements, 0, (this.elements = new Object[this.maxSize]), 0, this.size);
//        }
//        System.arraycopy(newVector.elements, 0, this.elements, this.size, newVector.size);
//        this.size += newVector.size;
        objects.addAll(newVector.objects);
    }

    /** Identity check */
    public boolean containsIdentical(Object element) {
        for (Object obj: objects)
            if (element == obj)
                return true;
        return false;
    }

    /** Equality check */
    public boolean contains(Object element) {
        return objects.contains(element);
    }

    public void copyInto(Object[] targetArray) {
        this.copyInto(targetArray, 0);
    }

    public void copyInto(Object[] targetArray, int index) {

        System.arraycopy(objects.toArray(), 0, targetArray, index, objects.size());
    }

    public Object elementAt(int index) {
        return objects.elementAt(index);
    }

    public Object find(Object element) {
        if (objects.contains(element)) {
            return element;
        }
        return null;
    }

    public Object remove(Object element) {
        // assumes only one occurrence of the element exists
//        for (int i = this.size; --i >= 0; )
//            if (element.equals(this.elements[i])) {
//                // shift the remaining elements down one spot
//                System.arraycopy(this.elements, i + 1, this.elements, i, --this.size - i);
//                this.elements[this.size] = null;
//                return element;
//            }
//        return null;
        if (objects.remove(element)) {
            return element;
        }
        return null;
    }

    public void removeAll() {
//
//        for (int i = this.size; --i >= 0; )
//            this.elements[i] = null;
//        this.size = 0;
        objects.clear();
    }

    public int size() {
        return objects.size();
    }

    public String toString() {
//        String s = ""; //$NON-NLS-1$
//        for (int i = 0; i < this.size; i++)
//            s += this.elements[i].toString() + "\n"; //$NON-NLS-1$
//        return s;
        return objects.toString();
    }
}
