/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid.api;

import static fish.payara.security.openid.api.OpenIdConstant.ADDRESS;
import static fish.payara.security.openid.api.OpenIdConstant.BIRTHDATE;
import static fish.payara.security.openid.api.OpenIdConstant.EMAIL;
import static fish.payara.security.openid.api.OpenIdConstant.EMAIL_VERIFIED;
import static fish.payara.security.openid.api.OpenIdConstant.FAMILY_NAME;
import static fish.payara.security.openid.api.OpenIdConstant.GENDER;
import static fish.payara.security.openid.api.OpenIdConstant.GIVEN_NAME;
import static fish.payara.security.openid.api.OpenIdConstant.LOCALE;
import static fish.payara.security.openid.api.OpenIdConstant.MIDDLE_NAME;
import static fish.payara.security.openid.api.OpenIdConstant.NAME;
import static fish.payara.security.openid.api.OpenIdConstant.NICKNAME;
import static fish.payara.security.openid.api.OpenIdConstant.PHONE_NUMBER;
import static fish.payara.security.openid.api.OpenIdConstant.PHONE_NUMBER_VERIFIED;
import static fish.payara.security.openid.api.OpenIdConstant.PICTURE;
import static fish.payara.security.openid.api.OpenIdConstant.PREFERRED_USERNAME;
import static fish.payara.security.openid.api.OpenIdConstant.PROFILE;
import static fish.payara.security.openid.api.OpenIdConstant.UPDATED_AT;
import static fish.payara.security.openid.api.OpenIdConstant.WEBSITE;
import static fish.payara.security.openid.api.OpenIdConstant.ZONEINFO;
import javax.json.JsonObject;

/**
 * User Claims received from the userinfo endpoint
 *
 * @author Gaurav Gupta
 */
public class OpenIdClaims {

    // profile scope claims
    private String name;
    private String familyName;
    private String givenName;
    private String middleName;
    private String nickname;
    private String preferredUsername;
    private String profile;
    private String picture;
    private String website;
    private String gender;
    private String birthdate;
    private String zoneinfo;
    private String locale;
    private String updatedAt;

    // email scope claims
    private String email;
    private String emailVerified;

    // address scope claims
    private String address;
    
    // phone scope claims
    private String phoneNumber;
    private String phoneNumberVerified;

    public OpenIdClaims(JsonObject claims) {
        this.name = claims.getString(NAME, null);
        this.familyName = claims.getString(FAMILY_NAME, null);
        this.givenName = claims.getString(GIVEN_NAME, null);
        this.middleName = claims.getString(MIDDLE_NAME, null);
        this.nickname = claims.getString(NICKNAME, null);
        this.preferredUsername = claims.getString(PREFERRED_USERNAME, null);
        this.profile = claims.getString(PROFILE, null);
        this.picture = claims.getString(PICTURE, null);
        this.website = claims.getString(WEBSITE, null);
        this.gender = claims.getString(GENDER, null);
        this.birthdate = claims.getString(BIRTHDATE, null);
        this.zoneinfo = claims.getString(ZONEINFO, null);
        this.locale = claims.getString(LOCALE, null);
        this.updatedAt = claims.getString(UPDATED_AT, null);
        this.email = claims.getString(EMAIL, null);
        this.emailVerified = claims.getString(EMAIL_VERIFIED, null);
        this.address = claims.getString(ADDRESS, null);
        this.phoneNumber = claims.getString(PHONE_NUMBER, null);
        this.phoneNumberVerified = claims.getString(PHONE_NUMBER_VERIFIED, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getZoneinfo() {
        return zoneinfo;
    }

    public void setZoneinfo(String zoneinfo) {
        this.zoneinfo = zoneinfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(String emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumberVerified() {
        return phoneNumberVerified;
    }

    public void setPhoneNumberVerified(String phoneNumberVerified) {
        this.phoneNumberVerified = phoneNumberVerified;
    }

    @Override
    public String toString() {
        return OpenIdClaims.class.getSimpleName()
                + "{"
                + "name=" + name
                + ", familyName=" + familyName
                + ", givenName=" + givenName
                + ", middleName=" + middleName
                + ", nickname=" + nickname
                + ", preferredUsername=" + preferredUsername
                + ", profile=" + profile
                + ", picture=" + picture
                + ", website=" + website
                + ", gender=" + gender
                + ", birthdate=" + birthdate
                + ", zoneinfo=" + zoneinfo
                + ", locale=" + locale
                + ", updatedAt=" + updatedAt
                + ", email=" + email
                + ", emailVerified=" + emailVerified
                + ", address=" + address
                + ", phoneNumber=" + phoneNumber
                + ", phoneNumberVerified=" + phoneNumberVerified
                + '}';
    }

}
