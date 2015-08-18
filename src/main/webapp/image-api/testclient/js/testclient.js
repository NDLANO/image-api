function searchIfEnter(event){
    if(event.keyCode == 13){
        $("#searchButton").click();
    }
}

function search() {
    var tagString = $('#tags').val();
    var minSize = $('#minSize').val();
    var searchUrl = "/images"

    if(tagString) {
        searchUrl = searchUrl.concat("?tags=", tagString)
    }
    if(minSize) {
        searchUrl = searchUrl.concat("&minimumSize=", minSize)
    }

    var request = window.superagent;
    request.get(searchUrl).end(
        function(err, res) {
            $('#searchresults').empty();
            $('#searchresults').append(res.text);
        }
    )
}