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
*/ /*
From docker source code 'runconfig/config.go':
ExposedPorts    map[nat.Port]struct{}

ExposedPorts in JSON response from docker remote API:
...,
"ExposedPorts":{
    "22/tcp": {}
},
...

It seems struct{} is reserved for future but it isn't in use for now.
*/
public class ExposedPort {
    public String toString() {
        return "{}";
    }
}
