## Todo

The current implementation does some inline functions to handle the mapping towards stapler. Its a little depreciated, though pretty clean solution.

Looks like the new method is

```
FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);
```

But havn't looked into it yet
