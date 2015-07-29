/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.docker.json;

/**
 * @author andrew00x
 */
/*
From docker source code 'runconfig/config.go':
Volumes         map[string]struct{}

Volumes in JSON response from docker remote API:
...,
"Volumes":{
        "/tmp": {}
 },
...

It seems struct{} is reserved for future but it isn't in use for now.
*/
public class Volume {
    public String toString() {
        return "{}";
    }
}
