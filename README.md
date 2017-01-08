# FlowLayout

A FlowLayout for Android, which allows child views flow to next row when there is no enough space. The spacing between child views can be calculated by the FlowLayout so that the views are evenly placed.

<p align="center">
<img src="images/sample.png" width="360"/>
</p>

## Attributes

| Attribute              | Format                   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| flow                   | boolean                  | `true` to allow flow. `false` to restrict all child views in one row.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| childSpacing           | `auto`/dimension         | The horizontal spacing between child views. Either `auto`, or a fixed size. `auto` means that the actual spacing is calculated according to the width of the container and the number of the child views in the row, so that the child views are placed evenly in the container.                                                                                                                                                                                                                                                                             |
| childSpacingForLastRow | `auto`/`align`/dimension | The horizontal spacing between child views of the last row. Either `auto`, `align` or a fixed size. `auto` means that the actual spacing is calculated according to the width of the container and the number of the child views in the row, so that the child views are placed evenly in the container. `align` means that the horizontal spacing of the child views in the last row keeps the same with the spacing used in the row above. If there is only one row, this value is ignored and the spacing will be calculated according to `childSpacing`. |
| rowSpacing             | `auto`/dimension         | The vertical spacing between rows. Either `auto`, or a fixed size. `auto` means that the actual spacing is calculated according to the height of the container and the number of rows, so that the rows are placed evenly in the container.                                                                                                                                                                                                                                                                                                                  |


## Licence

```
Copyright 2016 nex3z

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
