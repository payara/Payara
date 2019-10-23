# Monitoring Console Webapp JavaScript Documentation

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
settings        = { display }
display         = boolean
```
* `id` is derived from `name` and used as attribute name in `pages` object
* `widgets` can be ommitted for an empty page
* `numberOfColumns` can be ommitted
* `widgets` is allowed to be an array - if so it is made into an object using each widget's `series` for the attribute name

### Widget Model

```
WIDGET     = { series, type, target, grid, options }
series     = string
target     = string
type       = 'line' | 'bar'
grid       = { item, column, span }
item       = number
column     = number
span       = span
options    = { 
	drawMinLine:boolean,
	drawMaxLine:boolean,
	drawAvgLine:boolean,
	perSec:boolean,
	beginAtZero:boolean,
	autoTimeTicks:boolean,
	drawCurves:boolean,
	drawAnimations:boolean,
	rotateTimeLabels:boolean,
	drawPoints:boolean,
	drawFill:boolean,
	drawStableLine:boolean,
	showLegend:boolean,
	showTimeLabels:boolean
}

```
* `target` is derived from `series` if not present
* if `type` is not set `'line'` is assumed and set
* if `options` is ommitted it is initialised setting `beginAtZero` and `autoTimeTicks` to `true`


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
SERIES               = { series, instance, points, observedMax, observedMin, observedSum, observedValues, observedValueChanges, observedSince, stableCount, stableSince, legend }
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
```
* `Chart` is a chart object created by Chart.js
* Note that `series` is the actual ID of a stored series that can differ from `widget.series` in case the widget uses a pattern that matches one or more stored series
* `points` are given in a 1-dimensional array with alternating x and y values (x being the timestamp in ms since 1970)
* the `SERIES.legend` is set by the client while all other attributes are data send by the server. The details of `LEGEND_ITEM` can be found below.


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



## Data Driven UI Components
Code can be found in `md-view-components.js`.

The general idea of model driven UI components is that a model - usually a JS object - describes the UI with data. This data is passed to the component which composes the actual DOM elements based on this model. This decouples the caller from the details of the DOM and creates a defined boundary that helps to not leak details to calling code. The following will describe the API of such boundaries.

### Settings API
Describes the model expected by the `Settings` component.

```
SETTINGS = [GROUP]
GROUP    = { id, caption, entries }
id 		 = string
caption  = string
entries  = [ENTRY]
ENTRY    = { label, type, input, value, min, max, options, onChange } 
label    = string
type     = string
value    = number | string
min      = number
max      = number
options  = { *:string }
input    = fn () => string | fn () => jquery | string | jquery
onChange = fn (widget, newValue) => () | fn (newValue) => ()
```
When `caption` is provided this adds a _header_ entry identical to adding a _header_ entry explicitly as first element of the `entries` array.

The `options` object is used as map where the attribute names are the values of the options and the attribute values are the _string_ labels displayed for that option.

Mandatory members of `ENTRY` depend on `type` member. Variants are:
```
'header'   : { label }
'checkbox' : { label, value, onChange }
'range'    : { label, value, min, max, onChange }
'dropdown' : { label, value, options, onChange }
```
An `ENTRY` with no `type` is a _header_ in case `input` is not defined and a generic component if
`input` is defined:
```
<generic> : { label, input }
```
In other words the `type` is an indicator for non generic entries. The provided types exist to avoid duplication and consistency for reoccuring input elements. Since `input` could also be just a _string_ generic entries can be used for simple key-value entries.


### Legend API
Describes the model expected by the `Legend` component.

```
LEGEND          = [LEGEND_ITEM]
LEGEND_ITEM     = { label, value, color, backgroundColor }
label           = string
value           = string | number
color           = string
backgroundColor = string
```
* If `value` is a _string_ only the first word is displayed large.
This is as steight forward as it looks. All members are required. 
The model creates a new jquery object that must be inserted into the DOM by the caller.



### Navigation API
Describes the model expected by the `Navigation` component.

```
NAVIGATION = { pages, onChange }
pages      = [PAGE_ITEM]
PAGE_ITEM  = { label, id, active }
label      = string
id         = string
active     = boolean
onChange   = fn (id) => () 
```
* `onChange` is called when another page is selected passing the `PAGE_ITEM.id` of the selected page.