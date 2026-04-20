package com.bpanda.keycloak.model;

import org.keycloak.models.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class EMailUser implements UserModel {
    private String email;
    private final String id;

    public EMailUser(String email) {
        this.email = email;
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return this.id;
    }
    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    @Override
    public void setEmailVerified(boolean verified) {

    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return Stream.empty();
    }

    @Override
    public void joinGroup(GroupModel group) {

    }

    @Override
    public void leaveGroup(GroupModel group) {

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return false;
    }

    @Override
    public String getFederationLink() {
        return "";
    }

    @Override
    public void setFederationLink(String link) {

    }

    @Override
    public String getServiceAccountClientLink() {
        return "";
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {

    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
    @Override
    public void setUsername(String s) {
        throw new UnsupportedOperationException("Not supported for EMailUser");
    }

    @Override
    public Long getCreatedTimestamp() {
        return 0L;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public void setSingleAttribute(String name, String value) {

    }

    @Override
    public void setAttribute(String name, List<String> values) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public String getFirstAttribute(String name) {
        return "";
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Map.of();
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return Stream.empty();
    }

    @Override
    public void addRequiredAction(String action) {

    }

    @Override
    public void removeRequiredAction(String action) {

    }

    @Override
    public String getFirstName() {
        return "";
    }

    @Override
    public void setFirstName(String firstName) {

    }

    @Override
    public String getLastName() {
        return "";
    }

    @Override
    public void setLastName(String lastName) {

    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {

    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {

    }
}
