/* eslint no-console: 0 */
// Borrowed from https://github.com/OneGraph/graphiql-explorer-example
import React, { Component } from "react";
import "whatwg-fetch";
import GraphiQL from "graphiql";
import GraphiQLExplorer from "graphiql-explorer";
import CodeExporter from 'graphiql-code-exporter'
import snippets from 'graphiql-code-exporter/lib/snippets'
import { buildClientSchema, getIntrospectionQuery, parse } from "graphql";

import { makeDefaultArg, getDefaultScalarArgValue } from "./CustomArgs";

import "graphiql/graphiql.css";
import "./App.css";
import 'codemirror/theme/neo.css'
import 'graphiql-code-exporter/CodeExporter.css';

import type { GraphQLSchema } from "graphql";

function fetcher(params: Object): Object {
  return fetch(
    `${window.rootURL}/graphql/`,
    {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify(params)
    }
  )
    .then(function(response) {
      return response.text();
    })
    .then(function(responseBody) {
      try {
        return JSON.parse(responseBody);
      } catch (e) {
        return responseBody;
      }
    });
}

const DEFAULT_QUERY = `# shift-option/alt-click on a query below to jump to it in the explorer
# option/alt-click on a field in the explorer to select all subfields
query allItems {
  allItems {
    name
    id
    _class
    actions {
      _class
    }
    description
    displayName
    fullDisplayName
    url
    ... on hudson_model_Job {
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
          timestamp
        }
        keepLog
        number
        queueId
        result
        timestamp
      }
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
}`;

type State = {
  schema: ?GraphQLSchema,
  query: string,
  explorerIsOpen: boolean
};

class App extends Component<{}, State> {
  // eslint-disable-next-line react/sort-comp
  _graphiql: GraphiQL;
  state = { schema: null, query: DEFAULT_QUERY, explorerIsOpen: true, codeExporterIsVisible: false };

  handleToggleCodeExporter = () => this.setState({
    codeExporterIsVisible: !this.state.codeExporterIsVisible
  })

  componentDidMount() {
    fetcher({
      query: getIntrospectionQuery()
    }).then(result => {
      const editor = this._graphiql.getQueryEditor();
      editor.setOption("extraKeys", {
        ...(editor.options.extraKeys || {}),
        "Shift-Alt-LeftClick": this.handleInspectOperation
      });

      this.setState({ schema: buildClientSchema(result.data) });
    });
  }

  handleInspectOperation = (
    cm: any,
    mousePos: { line: Number, ch: Number }
  ) => {
    const parsedQuery = parse(this.state.query || "");

    if (!parsedQuery) {
      console.error("Couldn't parse query document");
      return null;
    }

    const token = cm.getTokenAt(mousePos);
    const start = { line: mousePos.line, ch: token.start };
    const end = { line: mousePos.line, ch: token.end };
    const relevantMousePos = {
      start: cm.indexFromPos(start),
      end: cm.indexFromPos(end)
    };

    const position = relevantMousePos;

    const def = parsedQuery.definitions.find(definition => {
      if (!definition.loc) {
        console.log("Missing location information for definition");
        return false;
      }

      return definition.loc.start <= position.start && definition.loc.end >= position.end;
    });

    if (!def) {
      console.error(
        "Unable to find definition corresponding to mouse position"
      );
      return null;
    }

    const operationKind =
      def.kind === "OperationDefinition"
        ? def.operation
        : def.kind === "FragmentDefinition"
        ? "fragment"
        : "unknown";

    const operationName =
      def.kind === "OperationDefinition" && Boolean(def.name)
        ? def.name.value
        : def.kind === "FragmentDefinition" && Boolean(def.name)
        ? def.name.value
        : "unknown";

    const selector = `.graphiql-explorer-root #${operationKind}-${operationName}`;

    const el = document.querySelector(selector);
    if (el) {
      el.scrollIntoView();
    }
  };

  handleEditQuery = (query: string): void => this.setState({ query });

  handleToggleExplorer = () => {
    this.setState({ explorerIsOpen: !this.state.explorerIsOpen });
  };

  render() {
    const { query, schema, codeExporterIsVisible } = this.state;

    const codeExporter = codeExporterIsVisible ? (
      <CodeExporter
        hideCodeExporter={this.handleToggleCodeExporter}
        serverUrl={window.location.protocol + '//' + window.location.host + document.head.dataset.rooturl + '/graphql/'}
        snippets={snippets}
        query={query}
        codeMirrorTheme="neo"
      />
    ) : null


    return (
      <div className="graphiql-container">
        <GraphiQLExplorer
          schema={schema}
          query={query}
          onEdit={this.handleEditQuery}
          onRunOperation={operationName =>
            this._graphiql.handleRunQuery(operationName)
          }
          explorerIsOpen={this.state.explorerIsOpen}
          onToggleExplorer={this.handleToggleExplorer}
          getDefaultScalarArgValue={getDefaultScalarArgValue}
          makeDefaultArg={makeDefaultArg}
        />
        <GraphiQL
          ref={ref => (this._graphiql = ref)}
          fetcher={fetcher}
          schema={schema}
          query={query}
          onEditQuery={this.handleEditQuery}
        >
          <GraphiQL.Toolbar>
            <GraphiQL.Button
              onClick={() => this._graphiql.handlePrettifyQuery()}
              label="Prettify"
              title="Prettify Query (Shift-Ctrl-P)"
            />
            <GraphiQL.Button
              onClick={() => this._graphiql.handleToggleHistory()}
              label="History"
              title="Show History"
            />
            <GraphiQL.Button
              onClick={this.handleToggleExplorer}
              label="Explorer"
              title="Toggle Explorer"
            />
            <GraphiQL.Button
              onClick={this.handleToggleCodeExporter}
              label="Code Exporter"
              title="Toggle Code Exporter"
            />
          </GraphiQL.Toolbar>
        </GraphiQL>
        {codeExporter}
      </div>
    );
  }
}

export default App;
