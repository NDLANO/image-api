function searchIfEnter(event){
    if(event.keyCode == 13){
        $("#searchButton").click();
    }
}

function searchOk(jsonData){
    $('#searchresults').empty();
    $.each(jsonData, function(index, element) {
        var previewImg = "<a href='#'><img src='" + element["previewUrl"] + "'/></a>"

        $('#searchresults').append(previewImg);
    });
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
            if(res.ok){
                searchOk(res.body);
            } else {
                console.log("Dette gikk ikke bra...");
            }
        }
    )
}