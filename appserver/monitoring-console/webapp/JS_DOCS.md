
## Model Driven UI Components
Code can be found in `md-view-components.js`.

The general idea of model driven UI components is that a model - usually a JS object - describes the UI with data. This data is passed to the component which composes the actual DOM elements based on this model. This decouples the caller from the details of the DOM and creates a defined boundary that helps to not leak details to calling code. The following will describe the API of such boundaries.

### Settings API
Describes the `model` expected by the `Settings` component.

```
Model    = [Group]
Group    = { id, caption, entries }
id 		 = string
caption  = string
entries  = [Entry]
Entry    = { label, type, input, value, min, max, options, onChange } 
label    = string
type     = string
value    = number | string
min      = number
max      = number
options  = { * }
input    = fn () => string | fn () => jquery | string | jquery
onChange = fn (widget, newValue) => ()
```
When `caption` is provided this adds a _header_ entry identical to adding a _header_ entry explicitly as first element of the `entries` array.

The `options` object is used as map where the attribute names are the values of the options and the attribute values are the _string_ labels displayed for that option.

Mandatory members of `Entry` depend on `type` member. Variants are:
```
'header'   : { label }
'checkbox' : { label, value, onChange }
'range'    : { label, value, min, max, onChange }
'dropdown' : { label, value, options, onChange }
```
An `Entry` with no `type` is a _header_ in case `input` is not defined and a generic component if
`input` is defined:
```
<generic> : { label, input }
```
In other words the `type` is an indicator for non generic entries. The provided types exist to avoid duplication and consistency for reoccuring input elements. Since `input` could also be just a _string_ generic entries can be used for simple key-value entries.


### Legend API
Describes the `model` expected by the `Legend` component.

```
Model    = [Item]
Item     = { label, value, color }
label    = string
value    = string | number
color    = string
```
This is as steight forward as it looks. All members are required. 
The model creates a new jquery object that must be inserted into the DOM by the caller.


### Navigation API
Describes the `model` expected by the `Navigation` component.

```
Model    = { pages, onChange }
pages    = [Page]
Page     = { label, id, active }
label    = string
id       = string
active   = boolean
onChange = fn (page) => () 
```