# BuildCraft Reborn API (compile only)

The few interfaces of [BuildCraft Reborn](https://github.com/Reiko45/BuildCraft-Reborn)'s MJ power API that
Galacticraft's BuildCraft support is compiled against: `IMjConnector`, `IMjReceiver`, `IMjPassiveProvider` and
`IMjReadable` as they are, and the part of `MjAPI` with the MJ unit and the block capabilities. They are never packaged
into Galacticraft's jar: with BuildCraft Reborn installed its own classes are used, and without it the classes that use
them are never loaded.

The BuildCraft API is under the MIT license: Copyright (c) 2011-2017 SpaceToad and the BuildCraft Team. Permission is
hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission
notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS",
WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
