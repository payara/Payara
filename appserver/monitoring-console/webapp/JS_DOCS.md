# Monitoring Console Webapp JavaScript Documentation

## Notation
Short explanation on the data structure notation used in this document:

* `TYPE = ...`: `TYPE` (all upper case) is defined as...
* `TYPE = primitve`: having the stated primitive or build in JS type (number, string, boolean)
* `Type = Date`: having the stated JS class or enumeration (starts with upper case letter)
* `TYPE = { x, y, ... }`: ... an object with fields `x`, `y`, ...
* `TYPE = { x:type, y:TYPE, ... }`: ... an object with fields `x`, `y` given their types explicitly - these can either be primitive (lower case) or defined (upper case)
* `TYPE = { *:type }`: an object which can have any attribte but these do have the given type (again primitive or defined)
* `TYPE = [ Y ]`: ... an array with elements of type `Y`
* `TYPE = A | B`: ... either of type `A` or type `B` where both can be any of the other possible notations
* `TYPE = fn (A, B) => X`: a function accepting type `A` and `B` producing `X` 
* `Enumeration = 'a' | 'b'`: the possible string constants for the enumeration type
* `attribute = ...`: same as `TYPE` just that this type of that attribute within the previously defined object type


## Model Data Structures
Code can be found in `md-model.js`.

### Multipage Model
The UI model describes the content and state of the user configurable pages.
It can be exported to a JSON file and importad from JSON files.
The UI model is stored in local browser storage after change to preserve all
state changes in case of relead.

```
UI              = { pages, settings }
pages           = { *: PAGE }
PAGE            = { name, id, numberOfColumns, widgets }
name            = string
id              = string
numberOfColumns = number
widgets         = [WIDGET] | { *: WIDGET }
settings        = { display, home }
display         = boolean
home            = string
```
* `id` is derived from `name` and used as attribute name in `pages` object
* `widgets` can be ommitted for an empty page
* `numberOfColumns` can be ommitted
* `widgets` is allowed to be an array - if so it is made into an object using each widget's `series` for the attribute name
* `home` is the `PAGE.id` of the currently shown page

### Widget Model

```
WIDGET     = { series, type, unit, target, grid, axis, options, decorations, status }
series     = string
target     = string
type       = 'line' | 'bar'
unit       = 'count' | 'ms' | 'ns' | 'bytes' | 'percent'
grid       = { item, column, span }
item       = number
column     = number
span       = span
axis       = { min, max }
min        = number
max        = number
options    = { 
	drawMinLine:boolean,
	drawMaxLine:boolean,
	drawAvgLine:boolean,
	perSec:boolean,
	noCurves:boolean,
	drawPoints:boolean,
	noFill:boolean,
	noTimeLabels:boolean
}
decorations= { waterline, thresholds }
waterline  = number
thresholds = { reference, alarming, critical }
reference  = 'off' | 'now' | 'min' | 'max' | 'avg'
alarming   = THRESHOLD
critical   = THRESHOLD
THRESHOLD  = { 
	value:number, 
	color:string,
	display:boolean 
}
status     = { *:STAUS }
STATUS     = { hint }
hint       = string
```
* `target` is derived from `series` if not present
* if `type` is not set `'line'` is assumed and set
* if `options`, `grid`, `decorations` or `THRESHOLD` fields aren't defined they are initialised to `{}`
* `status` is a map from assessment status (key) to a `STATUS` object to add information on the particular status used to help the user to make sense of the current status. For possible keys are those of `Status`


#### Decorations
Decorations are visual elements added to a graph that should help the viewer to
make sense of the numbers shown.

A `waterline` marks a baseline or limit value that should be marked in the graph with a extra line.

The `alarming` and `critical` can be used to to devide the graph into 3 classified areas.

When `alarming` is less then `critical`
* _normal_ is `alarming` and below
* _alarming_ is `alarming`-`critical`
* _critical_ is `critical` and above

When `alarming` is greater then `critical`
* _normal_ is `alarming` and above
* _alarming_ is `critical`-`alarming`
* _critical_ is `critical` and below



### Grid Model
The grid model is returned by the model API whenever the model is changed in a way that may require to update the entire page DOM. 
The grid model is created on the fly based on the view model, used by the view to update the DOM and discarded afterwards.

```
GRID    = [COLUMN]
COLUMN  = [CELL]
CELL    = undefined | null | { span, widget }
span    = number
widget  = WIDGET
```
* all columns have the same amount of `CELL` items
* an object `CELL` marks the top left corner cell of the widget in case it does span more cells
* a `undefined` `CELL` is a cell spanned by another cell above it or left of it
* a `null` `CELL` is an empty cell
* `span` is given explicitly as it can differ from the `widget.grid.span` target due to chosen number of columns on the page



### Update Data Structure
An update object is assembled when widget chart data is received from the backend and send to the view to update accordingly.

```
UPDATE               = { widget, data, chart }
widget               = WIDGET
data                 = { *:[SERIES] }
chart                = fn () => Chart
SERIES               = { series, instance, points, observedMax, observedMin, observedSum, observedValues, observedValueChanges, observedSince, stableCount, stableSince, legend, assessments }
series               = string
instance             = string
points               = [number]
observedMax          = number
observedMin          = number
observedSum          = number
observedValues       = number
observedValueChanges = number
observedSince        = number
stableCount          = number
stableSince          = number
legend               = LEGEND_ITEM
assessments          = ASSESSMENTS
```
* `Chart` is a chart object created by Chart.js
* Note that `series` is the actual ID of a stored series that can differ from `widget.series` in case the widget uses a pattern that matches one or more stored series
* `points` are given in a 1-dimensional array with alternating x and y values (x being the timestamp in ms since 1970)
* `SERIES.legend` and `SERIES.assessments` are set by the client 


## Chart Data
Documents the data as used and expected by Chart.js charts. 
The format depends on the type of chart used. 
Starting point for the chart data is the `UPDATE.data.points` (see above) that are transformed to the format expected by the chart type.

### Line Chart Data
Line charts are drawn for widgets of `WIDGET.type` `'line'`. Data uses 2-dimensional points with `x` and `y` component. In case of a time axis for the x-axis `x` is called `t`.
```
LINE_DATA = [POINT]
POINT     = { t, y }
t         = Date
y         = number
```
* multiple lines are created by creating multiple _datasets_ (Chart.js ) each having `LINE_DATA` value.

### Bar Chart Data
While bar charts do have two axis only one is dependent on the data points.
Therefore they use only a plain value for each point that corresponds to the length of the bar.
```
BAR_DATA = [value]
value    = number
``` 
* multiple bars are created by an `BAR_DATA` array with multiple `value`s. 
* stacked bars are created by multiple _datasets_ (Chart.js ) each holding same number of values.


### Assessment Information
Assessments are evaluations made by the client to classify the data based on a widgets configuration.

```
ASSESSMENTS = { status }
status      = Status
Status      = 'normal' | 'alarming' | 'critical' | 'error' | 'missing'
```



## Data Driven UI Components
Code can be found in `md-view-components.js`.

The general idea of model driven UI components is that a model - usually a JS object - describes the UI with data. This data is passed to the component which composes the actual DOM elements based on this model. This decouples the caller from the details of the DOM and creates a defined boundary that helps to not leak details to calling code. The following will describe the API of such boundaries.

### Settings API
Describes the model expected by the `Settings` component.

```
SETTINGS    = [GROUP]
GROUP       = { id, caption, entries }
id 		    = string
caption     = string
entries     = [ENTRY]
ENTRY       = { label, type, input, value, unit, min, max, options, onChange, description } 
label       = string
type        = undefined | 'header' | 'checkbox' | 'range' | 'dropdown' | 'value' | 'text'
unit        = string
value       = number | string
min         = number
max         = number
options     = { *:string }
input       = fn () => string | fn () => jquery | string | jquery | [ENTRY]
onChange    = fn (widget, newValue) => () | fn (newValue) => ()
description = string
```
* When `caption` is provided this adds a _header_ entry identical to adding a _header_ entry explicitly as first element of the `entries` array.
* The `options` object is used as map where the attribute names are the values of the options and the attribute values are the _string_ labels displayed for that option.
* `description` is optional for any type of `ENTRY`

Mandatory members of `ENTRY` depend on `type` member. Variants are:
```
'header'   : { label }
'checkbox' : { label, value, onChange }
'range'    : { label, value, min, max, onChange }
'dropdown' : { label, value, options, onChange }
'value'    : { label, value, unit, onChange }
'text'     : { label, value, onChange }
```
* Settings of type `'value'` are inputs for a number that depends on the `unit` 
used by the widget range. E.g. a duration in ms or ns, a size in bytes, a percentage or a plain number. The actual input component created will therefore depend on the `unit` provided.
If no unit is provided or the unit is undefined a plain number is assumed.

An `ENTRY` with no `type` is a _header_ in case `input` is not defined and a generic component if
`input` is defined:
```
<generic> : { label, input }
```
In other words the `type` is an indicator for non generic entries. The provided types exist to avoid duplication and consistency for reoccuring input elements. Since `input` could also be just a _string_ generic entries can be used for simple key-value entries.

When a generic input is an array of `ENTRY` the entries the `label` is optional.
When provided the `label` is used before the input component.



### Legend API
Describes the model expected by the `Legend` component.

```
LEGEND          = [LEGEND_ITEM]
LEGEND_ITEM     = { label, value, color, backgroundColor, assessments }
label           = string
value           = string | number
color           = string
backgroundColor = string
assessments     = ASSESSMENTS
```
* If `value` is a _string_ only the first word is displayed large.
This is as steight forward as it looks. All members are required. 
The model creates a new jquery object that must be inserted into the DOM by the caller.
* `color` is the color of the line or bar used to indicate the item, 
* `backgroundColor` is the background color of the line or bar should it use a fill
* `assessments` help to understand or classify the given value qualitatively 


### Indicator API
Describes the model expected by the `Indicator` component.
This component gives feedback on the status of each widget.

```
INDICATOR = { status, text }
status    = Status
text      = string
```


### MENU API
Describes the model expected by the `MENU` component that is used for any of the text + icon menus or toolbars.

```
MENU         = { id, groups }
groups       = [BUTTON_GROUP | BUTTON]
BUTTON_GROUP = { icon, label, description, clickable, items }
items        = [BUTTON]
clickable    = boolean
BUTTON       = { icon, label, description, disabled, hidden, onClick }
icon         = string
label        = string
description  = string
disabled     = boolean
hidden       = boolean
onClick      = fn () => ()
```
* `id` is optional
* `description` is optional
* if item in `MENU` array has `items` it is a `BUTTON_GROUP` otherwise it is a `BUTTON`