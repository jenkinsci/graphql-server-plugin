# Graphql Server Plugin

A graphql implementation for jenkins.

## WARNING: This is currently a work in progress

## TODO

- [x] Remove hacked in security stuff entirely (mostly commented out atm)
- [x] Abstract classes should be handled the same as interfaces
- [x] Create whoami function to output who you are
- [x] Remove allFreestyleJobs, and just allow class to be passed into allJobs
- [x] Add "id" argument to allQueryTypes
- [x] Add all the query and pagination items to list[] fields?
- [ ] Add allBuildsForJobs?
- [ ] Add allTestsForJobs?
* [X] Fix index.jelly for description of plugin (good enough for release though)
- [ ] Log info for `"message": "Can't resolve '/allUsers[0]/property[1]'. Abstract type 'hudson_model_UserProperty' must resolve to an Object type at runtime for field 'null.property'. Could not determine the exact type of 'hudson_model_UserProperty'"` (and fix problem)
- [ ] Fix `Name "__hudson_model_Action" must not begin with "__", which is reserved by GraphQL introspection. In a future release of graphql this will become a hard error.`
- [ ] Add https://github.com/OneGraph/graphiql-explorer (so rewrite ui with react/maven-frontend)

## "Feature Complete" query

```
query {
  allJobs {
    name
    id
    _class
    actions {
      _class
    }
    allBuilds {
      _class
      id
      actions {
        _class
        ... on hudson_tasks_junit_TestResultAction {
          _class
          failCount
          skipCount
          totalCount
          urlName
        }
        ... on hudson_model_CauseAction {
          causes {
            _class
            shortDescription
            ... on hudson_model_Cause_UserIdCause {
              userId
              userName
            }
          }
        }
      }
      artifacts {
        _class
        displayPath
        fileName
        relativePath
      }
      building
      description
      displayName
      duration
      estimatedDuration
      executor {
        _class
        currentExecutable {
          _class
        }
        idle
        likelyStuck
        number
        progress
      }
      fingerprint {
        _class
        fileName
        hash
        # original: BuildPtr
        timestamp
        # usage: [RangeItem]
      }
      fullDisplayName
      keepLog
      number
      queueId
      result
      timestamp
      url
    }
  }
  allUsers {
    _class
    absoluteUrl
    description
    fullName
    id
    property {
      _class
    }
  }
  whoAmI {
    anonymous
    authenticated
    authorities
    details
    name
    toString
  }
}
```

// FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
// environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);
