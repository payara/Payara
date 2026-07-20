# Design: Concurrent Resource Definitions in `payara-ejb-jar.xml`

**Date:** 2026-07-29  
**Tickets:** FISH-13126  
**Branches:** Payara 7 (main), Payara 6 (backport)

---

## Background

In Payara 6, the ability to define `managed-executor`, `managed-scheduled-executor`,
`managed-thread-factory`, and `context-service` via a deployment descriptor was
implemented by introducing a fabricated `ejb-jar_4_1.xsd`. This was incorrect: EJB
remains at version 4.0, and modifying Jakarta-owned schemas is prohibited.

The erroneous schema and all tests that depended on it were removed in commit
`70c6c744428fe5a7b4af06b3dd2795b45a109445` (FISH-13126). This design replaces
that implementation correctly.

---

## Why `payara-ejb-jar.xml` (not `payara-application.xml`)

**`ejb-jar_4_0.xsd`** — the standard EJB module descriptor — does **not** define
any of the four concurrent resource types. This is the gap the old fake schema
tried to fill.

**`application_10.xsd`** and **`application_11.xsd`** already define all four types
natively. Applications can and should use `application.xml` for EAR-level concurrent
resource definitions without any Payara extension. The existing concurrency sample's
`application.xml` demonstrates this working correctly.

Therefore:

- **`payara-ejb-jar.xml`** — new DTD version required; this is the only correct way
  to define concurrent resources at the EJB module level.
- **`payara-application.xml`** — intentionally excluded from this change. The
  standard `application.xml` already covers it. Documentation should explicitly note
  this so users aren't confused.

---

## Element Sets by Jakarta EE Version

The DTD elements must faithfully mirror the standard Jakarta EE schema for the
respective platform version. The sets differ between EE 10 and EE 11.

### Jakarta EE 10 — `jakartaee_10.xsd` (`managed-executorType` lines 1984–2081)

| Element | `managed-executor` | `managed-scheduled-executor` | `managed-thread-factory` | `context-service` |
|---|:---:|:---:|:---:|:---:|
| `description?` | ✓ | ✓ | ✓ | ✓ |
| `name` | ✓ | ✓ | ✓ | ✓ |
| `context-service-ref?` | ✓ | ✓ | ✓ | — |
| `max-async?` | ✓ | ✓ | — | — |
| `hung-task-threshold?` | ✓ | ✓ | — | — |
| `priority?` | — | — | ✓ | — |
| `cleared*` | — | — | — | ✓ |
| `propagated*` | — | — | — | ✓ |
| `unchanged*` | — | — | — | ✓ |
| `property*` | ✓ | ✓ | ✓ | ✓ |
| `qualifier*` | — | — | — | — |
| `virtual?` | — | — | — | — |

### Jakarta EE 11 — `jakartaee_11.xsd` (`managed-executorType` lines 2020–2153)

Same as EE 10, **plus**:

| Element | `managed-executor` | `managed-scheduled-executor` | `managed-thread-factory` | `context-service` |
|---|:---:|:---:|:---:|:---:|
| `qualifier*` | ✓ | ✓ | ✓ | ✓ |
| `virtual?` | ✓ | ✓ | ✓ | — |
| `property*` in `context-service` | — | — | — | **removed** (not in EE 11) |

---

## Payara 6 Changes

### New DTD: `payara6-ejb-jar_4_0-1.dtd`

Copy of `payara6-ejb-jar_4_0-0.dtd` with the following changes:

**Updated DOCTYPE string:**
```
-//Payara.fish//DTD Payara Application Server 6 EJB 4.0 Revision 1//EN
```

**Updated `enterprise-beans` content model** (preserving the existing
`pm-descriptors?` and `cmp-resource?` elements that are present in Payara 6
but absent in Payara 7):

```dtd
<!ELEMENT enterprise-beans (name?, unique-id?, ejb*, pm-descriptors?, cmp-resource?,
    message-destination*, managed-executor*, managed-scheduled-executor*,
    managed-thread-factory*, context-service*,
    webservice-description*, property*, webservice-default-login-config?)>
```

**New element declarations:**

```dtd
<!ELEMENT managed-executor (description?, name, context-service-ref?,
                             max-async?, hung-task-threshold?, property*)>
<!ELEMENT managed-scheduled-executor (description?, name, context-service-ref?,
                                      max-async?, hung-task-threshold?, property*)>
<!ELEMENT managed-thread-factory (description?, name, context-service-ref?,
                                   priority?, property*)>
<!ELEMENT context-service (description?, name, cleared*, propagated*, unchanged*,
                            property*)>

<!ELEMENT context-service-ref (#PCDATA)>
<!ELEMENT max-async (#PCDATA)>
<!ELEMENT hung-task-threshold (#PCDATA)>
<!ELEMENT priority (#PCDATA)>
<!ELEMENT cleared (#PCDATA)>
<!ELEMENT propagated (#PCDATA)>
<!ELEMENT unchanged (#PCDATA)>
```

Elements `name`, `description`, and `property` already exist in the DTD and are
reused without redeclaration.

### `DTDRegistry.java` — two new constants

```java
public static final String PAYARA6_EJBJAR_401_DTD_PUBLIC_ID =
    "-//Payara.fish//DTD Payara Application Server 6 EJB 4.0 Revision 1//EN";
public static final String PAYARA6_EJBJAR_401_DTD_SYSTEM_ID =
    "https://raw.githubusercontent.com/payara/Payara/refs/heads/main/appserver/deployment/dtds/src/main/resources/glassfish/lib/dtds/payara6-ejb-jar_4_0-1.dtd";
```

### `PayaraEjbBundleRuntimeNode` (Payara 6)

- `getDocType()` / `getSystemID()` → updated to `PAYARA6_EJBJAR_401_*`
- `registerBundle()` → add `PAYARA6_EJBJAR_401_*` mapping; retain `PAYARA6_EJBJAR_400_*`
  for backward compatibility (existing files with old DOCTYPE still parse)

---

## Payara 7 Changes

### New DTD: `payara7-ejb-jar_4_0-2.dtd`

Copy of `payara7-ejb-jar_4_0-1.dtd` with the following changes:

**Updated DOCTYPE string:**
```
-//Payara.fish//DTD Payara Application Server 7 EJB 4.0 Revision 2//EN
```

**Updated `enterprise-beans` content model:**

```dtd
<!ELEMENT enterprise-beans (name?, unique-id?, ejb*,
    message-destination*, managed-executor*, managed-scheduled-executor*,
    managed-thread-factory*, context-service*,
    webservice-description*, property*, webservice-default-login-config?)>
```

**New element declarations:**

```dtd
<!ELEMENT managed-executor (description?, name, context-service-ref?, qualifier*,
                             max-async?, hung-task-threshold?, virtual?, property*)>
<!ELEMENT managed-scheduled-executor (description?, name, context-service-ref?, qualifier*,
                                      max-async?, hung-task-threshold?, virtual?, property*)>
<!ELEMENT managed-thread-factory (description?, name, context-service-ref?, qualifier*,
                                   priority?, virtual?, property*)>
<!ELEMENT context-service (description?, name, qualifier*, cleared*, propagated*, unchanged*)>

<!ELEMENT context-service-ref (#PCDATA)>
<!ELEMENT max-async (#PCDATA)>
<!ELEMENT hung-task-threshold (#PCDATA)>
<!ELEMENT qualifier (#PCDATA)>
<!ELEMENT priority (#PCDATA)>
<!ELEMENT virtual (#PCDATA)>
<!ELEMENT cleared (#PCDATA)>
<!ELEMENT propagated (#PCDATA)>
<!ELEMENT unchanged (#PCDATA)>
```

Two notable differences from the Payara 6 DTD:
- `context-service` gains `qualifier*` but loses `property*` — this tracks `jakartaee_11.xsd`
  which removed `property*` from `context-serviceType` relative to EE 10.
- The executor and factory types gain `qualifier*` and `virtual?`.

### `DTDRegistry.java` — two new constants

```java
public static final String PAYARA7_EJBJAR_402_DTD_PUBLIC_ID =
    "-//Payara.fish//DTD Payara Application Server 7 EJB 4.0 Revision 2//EN";
public static final String PAYARA7_EJBJAR_402_DTD_SYSTEM_ID =
    "https://raw.githubusercontent.com/payara/Payara/refs/heads/main/appserver/deployment/dtds/src/main/resources/glassfish/lib/dtds/payara7-ejb-jar_4_0-2.dtd";
```

### `PayaraEjbBundleRuntimeNode` (Payara 7)

- `getDocType()` / `getSystemID()` → updated to `PAYARA7_EJBJAR_402_*`
- `registerBundle()` → add `PAYARA7_EJBJAR_402_*` mapping; retain all prior
  mappings (`PAYARA6_EJBJAR_400_*`, `PAYARA_EJBJAR_400_*`, `PAYARA7_EJBJAR_401_*`)

---

## Shared Java Change: `EnterpriseBeansRuntimeNode`

This applies identically to both Payara 6 and Payara 7 branches.

`EnterpriseBeansRuntimeNode` is in `org.glassfish.ejb.deployment.node.runtime` — it
is Payara's own code, not upstream GlassFish. Four handler registrations are added
to its constructor:

```java
registerElementHandler(new XMLElement(TagNames.MANAGED_EXECUTOR),
    ManagedExecutorDefinitionNode.class, "addResourceDescriptor");
registerElementHandler(new XMLElement(TagNames.MANAGED_SCHEDULED_EXECUTOR),
    ManagedScheduledExecutorDefinitionNode.class, "addResourceDescriptor");
registerElementHandler(new XMLElement(TagNames.MANAGED_THREAD_FACTORY),
    ManagedThreadFactoryDefinitionNode.class, "addResourceDescriptor");
registerElementHandler(new XMLElement(TagNames.CONTEXT_SERVICE),
    ContextServiceDefinitionNode.class, "addResourceDescriptor");
```

**Why this works without new classes:**

- All four node classes (`ManagedExecutorDefinitionNode`, etc.) already exist in
  `com.sun.enterprise.deployment.node` on both branches.
- `EnterpriseBeansRuntimeNode.getDescriptor()` delegates to
  `getParentNode().getDescriptor()`, returning the `EjbBundleDescriptorImpl`.
- `EjbBundleDescriptorImpl` extends `BundleDescriptor`, which has
  `addResourceDescriptor()` — the same method used by `ApplicationNode` for the
  same four resource types.
- No new classes or method signatures are needed.

---

## Example `payara-ejb-jar.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE payara-ejb-jar PUBLIC
  "-//Payara.fish//DTD Payara Application Server 7 EJB 4.0 Revision 2//EN"
  "https://raw.githubusercontent.com/payara/Payara/refs/heads/main/appserver/deployment/dtds/src/main/resources/glassfish/lib/dtds/payara7-ejb-jar_4_0-2.dtd">
<payara-ejb-jar>
  <enterprise-beans>
    <managed-executor>
      <name>java:app/jakartaee/EJBExecutor</name>
      <max-async>2</max-async>
      <hung-task-threshold>120000</hung-task-threshold>
    </managed-executor>
    <managed-scheduled-executor>
      <name>java:app/jakartaee/CustomManagedScheduledExecutorE</name>
      <max-async>5</max-async>
      <hung-task-threshold>120000</hung-task-threshold>
    </managed-scheduled-executor>
    <managed-thread-factory>
      <name>java:app/jakartaee/ManagedThreadFactoryE</name>
      <priority>4</priority>
    </managed-thread-factory>
    <context-service>
      <name>java:global/concurrent/ContextE</name>
      <cleared>IntContext</cleared>
      <propagated>Application</propagated>
      <propagated>IntContextProvider</propagated>
      <unchanged>Transaction</unchanged>
    </context-service>
  </enterprise-beans>
</payara-ejb-jar>
```

---

## Test Reinstatement — Payara Concurrency Sample

The following classes were deleted in commit `70c6c74` and must be re-added to
`appserver/tests/payara-samples/samples/concurrency/src/main/java/`:

| Class | Package | What it does |
|---|---|---|
| `ManagedExecutorDefinitionEJBFromConfig` | `...managedexecutor` | `@Stateless` EJB that `@Resource`-injects a `ManagedExecutorService` defined in `payara-ejb-jar.xml` |
| `ManagedScheduledExecutorEJBFromConfig` | `...managedscheduledexecutor` | Same for `ManagedScheduledExecutorService` |
| `ManagedThreadFactoryEJBFromConfig` | `...managedthreadfactory` | Same for `ManagedThreadFactory` |
| `ContextServiceEJBFromConfig` | `...contextservice` | `@Stateless` EJB that uses a `ContextService` defined in `payara-ejb-jar.xml` |

A single `payara-ejb-jar.xml` test resource (under `src/test/resources/`) replaces
the four deleted `ejb-jar.xml` / `ejb-jar2.xml` / `ejb-jar3.xml` / `ejb-jar4.xml`
files, defining all four resources together.

The `*ApplicationIT` integration tests that covered the EJB-from-config cases need
their deployment methods updated to include the new `payara-ejb-jar.xml` in the
`JavaArchive` (`ejb-jar.jar`) and to re-add the `*EJBFromConfig` classes.

---

## Documentation

Two documentation additions are required:

1. **`payara-ejb-jar.xml` reference page** — add a "Concurrent Resources" section
   documenting `managed-executor`, `managed-scheduled-executor`,
   `managed-thread-factory`, and `context-service` with element tables and the
   example above.

2. **`payara-application.xml` reference page** — add a note explicitly stating that
   concurrent resource definitions belong in the standard `application.xml` (not the
   Payara companion), with a reference to the Jakarta EE Concurrency specification
   and the relevant section of `application_11.xsd`.

---

## File Change Summary

### Payara 7 (main)

| File | Change |
|---|---|
| `appserver/deployment/dtds/.../payara7-ejb-jar_4_0-2.dtd` | New file |
| `appserver/deployment/dol/.../xml/DTDRegistry.java` | Add `PAYARA7_EJBJAR_402_*` constants |
| `.../ejb/deployment/node/runtime/PayaraEjbBundleRuntimeNode.java` | New DTD as default; register v402 in bundle map |
| `.../ejb/deployment/node/runtime/EnterpriseBeansRuntimeNode.java` | Add 4 handler registrations |
| `appserver/tests/payara-samples/samples/concurrency/...` | Re-add `*EJBFromConfig` classes + `payara-ejb-jar.xml` test resource; update ITs |
| Documentation pages | Add concurrent resource sections |

### Payara 6 (backport)

| File | Change |
|---|---|
| `appserver/deployment/dtds/.../payara6-ejb-jar_4_0-1.dtd` | New file |
| `appserver/deployment/dol/.../xml/DTDRegistry.java` | Add `PAYARA6_EJBJAR_401_*` constants |
| `.../ejb/deployment/node/runtime/PayaraEjbBundleRuntimeNode.java` | New DTD as default; register v401 in bundle map |
| `.../ejb/deployment/node/runtime/EnterpriseBeansRuntimeNode.java` | Add 4 handler registrations (identical to Payara 7) |
| `appserver/tests/payara-samples/samples/concurrency/...` | Same test changes as Payara 7 |
| Documentation pages | Same documentation additions |