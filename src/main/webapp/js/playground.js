window.addEventListener("load", function(event) {
    const loadingWrapper = document.getElementById("loading-wrapper");
    loadingWrapper.classList.add("fadeOut");
    const root = document.getElementById("root");
    root.classList.add("playgroundIn");
    GraphQLPlayground.init(root, {
        endpoint: window.rootURL + "/graphql/",
        settings: {
            "schema.polling.enable": false
        }
    });
});
