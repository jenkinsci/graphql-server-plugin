window.addEventListener("load", function() {
    const loadingWrapper = document.getElementById("loading-wrapper");
    loadingWrapper.classList.add("fadeOut");
    const root = document.getElementById("root");
    root.classList.add("playgroundIn");
    window.GraphQLPlayground.init(root, {
        endpoint: window.rootURL + "/graphql/",
        settings: {
            "request.credentials": "include",
            "schema.polling.enable": false
        }
    });
});
