# Monitoring Console Webapp JavaScript Documentation

## Notation
Short explanation on the data structure notation used in this document:

* `TYPE = ...`: `TYPE` (all upper case) is defined as...
* `TYPE = primitve`: having the stated primitive or build in JS type (number, string, boolean)
* `Type = Date`: having the stated JS class or enumeration (starts with upper case letter)
* `TYPE = { x, y, ... }`: ... an object with fields `x`, `y`, ...
* `TYPE = { x:type, y:TYPE, ... }`: ... an object with fields `x`, `y` given their types explicitly - these can either be primitive (lower case) or defined (upper case)
* `TYPE = { *:type }`: an object which can have any attribute but these do have the given type (again primitive or defined)
* `TYPE = [ Y ]`: ... an array with elements of type `Y`
* `TYPE = A | B`: ... either of type `A` or type `B` where both can be any of the other possible notations
* `TYPE = fn (A, B) => X`: a function accepting type `A` and `B` producing `X` 
* `Enumeration = 'a' | 'b'`: the possible string constants for the enumeration type
* `attribute = ...`: same as `TYPE` just that this type of that attribute within the previously defined object type


## Model Data Structures
Code can be found in `md-model.js`.

### Multipage Model
The UI model describes the content and state of the user configurable pages.
It can be exported to a JSON file and imported from JSON files.
The UI model is stored in local browser storage after change to preserve all
state changes in case of reload.

```
UI              = { pages, settings }
pages           = { *: PAGE }
PAGE            = { name, id, numberOfColumns, rotate, widgets }
name            = string
id              = string
numberOfColumns = number
rotate          = boolean
widgets         = [WIDGET] | { *: WIDGET }
settings        = { display, home, refresh, rotation, theme }
display         = boolean
home            = string
refresh         = { paused, interval }
rotation        = { enabled, interval }
paused          = boolean
enabled         = boolean
interval        = number
theme           = { palette, colors, options }
palette         = [COLOR]
colors          = { *:COLOR }
options         = { *:number }
COLOR           = string
```
* `id` is derived from `name` and used as attribute name in `pages` object
* `widgets` can be omitted for an empty page
* `numberOfColumns` can be omitted
* `widgets` is allowed to be an array - if so it is made into an object using each widget's `series` for the attribute name
* `home` is the `PAGE.id` of the currently shown page
* names for `defaults` colors used so far are: `'alarming'`, `'critical'` and `'waterline'`

### Widget Model

```
WIDGET     = { id, series, type, unit, scaleFactor, target, grid, axis, options, decorations, status, displayName, coloring, fields, mode, sort }
id         = string
series     = string
target     = string
displayName= string
type       = 'line' | 'bar' | 'alert' | 'annotation'
unit       = UNIT
UNIT       = 'count' | 'ms' | 'ns' | 'bytes' | 'percent'
coloring   = 'instance' | 'series' | 'index' | 'instance-series'
fields     = [ string ]
mode       = 'list' | 'table'
sort       = 'time' | 'value'
scaleFactor= number
grid       = { item, column, span, colspan, rowspan }
item       = number
column     = number
span       = number
colspan    = number
rowspan    = number
axis       = { min, max }
min        = number
max        = number
options    = { 
	drawMinLine:boolean,
	drawMaxLine:boolean,
	drawAvgLine:boolean,
	perSec:boolean,
	decimalMetric:boolean,
	drawCurves:boolean,
	drawPoints:boolean,
	noFill:boolean,
	noTimeLabels:boolean,
	noAnnotations:boolean,
}
decorations= { waterline, thresholds, alerts, annotations }
waterline  = { value:number color:string }
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
alerts     = { 
	noOngoing:boolean,
	noStopped:boolean,
	noAcknowledged:boolean,
	noUnacknowledged:boolean,
	noAmber:boolean,
	noRed:boolean,
}
annotations = { }
```
* `target` is derived from `series` if not present
* if `type` is not set `'line'` is assumed and set
* if `options`, `grid`, `decorations` or `THRESHOLD` fields aren't defined they are initialised to `{}`
* `status` is a map from assessment status (key) to a `STATUS` object to add information on the particular status used to help the user to make sense of the current status. For possible keys are those of `Status`
* `span` can be given instead of `colspan` and `rowspan` when they have the same value


#### Decorations
Decorations are visual elements added to a graph that should help the viewer to
make sense of the numbers shown.

A `waterline` marks a baseline or limit value that should be marked in the graph with a extra line.

The `alarming` and `critical` can be used to to divide the graph into 3 classified areas.

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
CELL    = undefined | null | { colspan, rowspan, widget }
colspan = number
rowspan = number
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
UPDATE               = { widget, chart, data, alerts, watches }
widget               = WIDGET
chart                = fn () => Chart
data                 = DATA
alerts               = [ALERT]
watches              = [WATCH]
```
* `Chart` is a chart object created by Chart.js

Series Data (as received from server):
```
DATA                 = { *:[SERIES] }
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
* Note that `series` is the actual ID of a stored series that can differ from `widget.series` in case the widget uses a pattern that matches one or more stored series
* `points` are given in a 1-dimensional array with alternating x and y values (x being the timestamp in ms since 1970)
* `SERIES.legend` and `SERIES.assessments` are set by the client 

Alert Data (as received from server):
```
ALERT                = { serial, level, series, instance, initiator, acknowledged, stopped, frames}
serial               = number
level                = string
series               = string
instance             = string
initiator            = WATCH
acknowledged         = boolean
stopped              = boolean
frames               = [FRAME]
FRAME                = { level, cause, captured, start, end }
cause                = DATA
captured             = [DATA]
start                = number
end                  = number
```

Watch Data (as received from server):
```
WATCH                = { name, series, unit, stopped, disabled, programmatic, red, amber, green, states }
name                 = string
series               = string
unit                 = string
stopped              = boolean
disabled             = boolean
programmatic         = boolean
red                  = CIRCUMSTANCE
amber                = CIRCUMSTANCE
green                = CIRCUMSTANCE
CIRCUMSTANCE         = { level, start, stop, suppress, surpressingSeries, surpressingUnit }
level                = string,
start                = CONDITION
stop                 = CONDITION
suppress             = CONDITION 
surpressingSeries    = string
surpressingUnit      = string
states               = SERIES_STATES
SERIES_STATES        = { *:INSTANCE_STATES }
INSTANCE_STATES      = { *:string }
```


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
Status      = 'normal' | 'alarming' | 'critical' | 'error' | 'missing' | 'white' | 'green' | 'amber' | 'red'
```



## Data Driven UI Components
Code can be found in `md-view-components.js`.

The general idea of model driven UI components is that a model - usually a JS object - describes the UI with data. This data is passed to the component which composes the actual DOM elements based on this model. This decouples the caller from the details of the DOM and creates a defined boundary that helps to not leak details to calling code. The following will describe the API of such boundaries.

### Settings API
Describes the model expected by the `Settings` component.

```
SETTINGS    = { id, groups }
groups      = [GROUP]
GROUP       = { id, caption, entries, collapsed }
id 		    = string
caption     = string
entries     = [ENTRY]
ENTRY       = { label, type, input, value, unit, min, max, options, onChange, description, defaultValue, collapsed } 
label       = string
type        = undefined | 'header' | 'checkbox' | 'range' | 'dropdown' | 'value' | 'text' | 'color'
unit        = string | fn () => string
value       = number | string
defaultValue= number | string
min         = number
max         = number
options     = { *:string } | [ * ]
input       = fn () => string | fn () => jquery | string | jquery | [ENTRY]
onChange    = fn (widget, newValue) => () | fn (newValue) => ()
description = string
collapsed   = boolean
```
* When `caption` is provided this adds a _header_ entry identical to adding a _header_ entry explicitly as first element of the `entries` array.
* The `options` object is used as map where the attribute names are the values of the options and the attribute values are the _string_ labels displayed for that option.
* `description` is optional for any type of `ENTRY`
* set `collapsed` to initially collapse the setting group

Mandatory members of `ENTRY` depend on `type` member. Variants are:
```
'header'   : { label, collapsed }
'checkbox' : { label, value, onChange }
'range'    : { label, value, min, max, onChange }
'dropdown' : { label, value, options, onChange }
'value'    : { label, value, unit, onChange }
'text'     : { label, value, onChange }
'color'    : { label, value, defaultValue, onChange }
```
* `onChange` may be omitted for _text_ inputs which makes the field _readonly_.
* Settings of type `'value'` are inputs for a number that depends on the `unit` 
used by the widget range. E.g. a duration in ms or ns, a size in bytes, a percentage or a plain number. The actual input component created will therefore depend on the `unit` provided.
If no unit is provided or the unit is undefined a plain number is assumed.

An `ENTRY` with no `type` is a _header_ in case `input` is not defined and a generic component if
`input` is defined:
```
<generic> : { label, input }
```
In other words the `type` is an indicator for non generic entries. The provided types exist to avoid duplication and consistency for reoccurring input elements. Since `input` could also be just a _string_ generic entries can be used for simple key-value entries.

When a generic input is an array of `ENTRY` the entries the `label` is optional.
When provided the `label` is used before the input component.



### Legend API
Describes the model expected by the `Legend` component.

```
LEGEND          = [LEGEND_ITEM]
LEGEND_ITEM     = { label, value, color, background, status, highlight }
label           = string | [ string ]
value           = string | number
color           = string
background      = string | [ string ]
status          = Status
highlight       = string

```
* If `value` is a _string_ only the first word is displayed large.
This is as straight forward as it looks. All members are required. 
The model creates a new jQuery object that must be inserted into the DOM by the caller.
* `color` is the color of the line or bar used to indicate the item, 
* `background` is the background color of the line or bar should it use a fill, if an array is used those are the start and end color of a linear gradient
* `highlight` is the color used to highlight the status of the text


### Indicator API
Describes the model expected by the `Indicator` component.
This component gives feedback on the status of each widget.

```
INDICATOR = { status, text, color }
status    = Status
text      = string
color     = string
```


### Menu API
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


### Alert Table API
Describes the model expected by the `AlertTable` component.
This component gives a tabular overview of alerts that occurred for the widget `series`.

```
ALERT_TABLE  = { id, verbose, items }
brief        = boolean
items        = [ ALERT_ITEM ]
ALERT_ITEM   = { serial, name, series, instance, unit, color, acknowledged, frames, watch, annotations }
serial       = number
name         = string
series       = string
instance     = instance
unit         = UNIT
acknowledged = boolean
frames       = [ALERT_FRAME]
ALERT_FRAME  = { level, since, until, color }
level        = 'red' | 'amber'
since        = date | number
until    	 = date | number
color        = string
watch        = { red, amber, green }
red          = CIRCUMSTANCE
amber        = CIRCUMSTANCE
green        = CIRCUMSTANCE
CIRCUMSTANCE = { start, stop }
start        = CONDITION
stop         = CONDITION
CONDITION    = { operator, threshold, forTimes, forMillis, onAverage }
annotations  = [ ANNOTATION ]
ANNOTATION   = { time, value, attrs, fields }
attrs        = { *:string }
fields       = [ string ]
```
* `verbose` default is `true`, `false` to get a condensed output that skips some of the properties in the output
* `since` is the start date as timestamp or JS date
* `ALERT_ITEM.color` refers to the series/instance color whereas `ALERT_FRAME.color` refers to the level color
* `items` and `frames` can be given in any order but might be changed to a particular order while processing the model
* only one of `forTimes`, `forPercent`, `forMillis` should be set


### Annotation Table API
Describes the model expected by the `AnnotationTable` component.
This component show the annotation for the widget series as a table of sorted entries.
The main information are the annotation attributes that are custom to each annotation origin.

```
ANNOTATION_TABLE = { id, mode, sort, items }
id               = string
mode             = 'list' | 'table'
sort             = 'value' | 'time'
items            = [ ANNOTATION_ITEM ]
ANNOTATION_ITEM  = { color, series, instance, unit, time, value, attrs, fields, formatters }
color            = string
series           = string
instance         = string
unit             = UNIT
time             = number
value            = number
attrs            = { *:string }
fields           = [ string ]
formatters       = [ ATTR_FORMATTER ]
ATTR_FORMATTER   = { applies, format, type }
applies          = fn (ANNOTATION_ITEM, string, string) => boolean
format           = fn (ANNOTATION_ITEM, string) => string
type             = undefined | 'pre'
```
* `series` and `instance` may be omitted (leaves out the info)
* `attrs` is a key-value map with `string` keys and values
* `type` and `background` can be omitted (assumes `'em'` and no background colour)
* `color` is the colour of the annotation item border indicator
* `fields` is a list of attribute keys that works as filter as well as giving the order; can be undefined or empty to use all attributes in their given order.



### Watch Manager API
Describes the model expected by the `WatchManager` component which combines the `WatchList` and `WatchBuilder` components.
The manager shows a configuration with a list of watches which also allows to create new watches.

```
WATCH_LIST      = { id, items, colors, actions  }
WATCH_BUILDER   = { id, colors, actions }
WATCH_MANAGER	= { id, items, colors, actions }
items           = [ WATCH ]
actions         = { onCreate, onDelete, onDisable, onEnable }
onEdit          = fn (WATCH) => ()
onCreate        = fn (WATCH, onSuccess, onFailure) => ()
onDelete        = fn (name, onSuccess, onFailure) => ()
onDisable       = fn (name, onSuccess, onFailure) => ()
onEnable        = fn (name, onSuccess, onFailure) => ()
colors          = { red, amber, green }
red             = string
amber           = string
green           = string
```
* `WATCH` refers to an object as described for update data structures
* `onDelete` is a function to call to delete the watch by its `name` (a `string`)
* `onCreate` is a function to create new watches
* `onEdit` is created by the `WatchManager` for the `WATCH_LIST` to use when an list entry should be edited.


## Data Driven Chart Plugins
Code can be found in `md-line-chart.js`.

These are additions to the drawing of charts where the drawn content is controlled by a data model computed from the widget `UPDATE`.

### Line Chart Background Areas 
Describes the model used to enhance the background of line charts with coloured areas on the Y-axis. The model is processed by a chart plugin to draw additional
decorations onto the chart background.

```
BACKGROUND_AREA = [ AREA_ITEM ];
AREA_ITEM       = { color, min, max, type, style }
color           = string
min             = number
max             = number
type            = 'lower' | 'upper'
style           = 'fill' | 'outline' 
```
* default for `type` is `upper`
* default for `style` is `fill`


