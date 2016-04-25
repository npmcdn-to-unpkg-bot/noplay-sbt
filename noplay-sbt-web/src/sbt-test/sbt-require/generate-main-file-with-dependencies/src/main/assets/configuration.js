define(function () {
    console.log('in configuration');
    require.config({
        config: {
            'index': {
                property1: "value1",
                property2: "value2"
            }
        }
    });
});