function showAdvancedSearch() {
    $('#advancedsearchbox').toggle("fast");
    $('#minSize').val('');
    $('#withLicense').val('');
    $('#inLanguage').val('');
}

function showImage(jsonData) {
    $('#imagedetail').show();

    $('#imagetitle').empty();
    $.each(jsonData["titles"], function(index, element) {
        $('#imagetitle').append(element["title"] + ' (' + element["language"] + ')', "<br/>");
    });

    $('#alttext').empty();
    $.each(jsonData["alttexts"], function(index, element) {
        $('#alttext').append(element["alttext"], "<br/>");
    });

    $('#imagesize').empty().append(jsonData["images"]["full"]["size"]);

    var license = '<a href="' + jsonData["copyright"]["license"]["url"] + '" target="_blank">' + jsonData["copyright"]["license"]["description"] + '</a>';
    $('#license').empty().append(license);

    var origin = '<a href="' + jsonData["copyright"]["origin"] + '" target="_blank">' + jsonData["copyright"]["origin"] + '</a>';
    $('#origin').empty().append(origin);

    $('#authors').empty();
    var numElements = jsonData["copyright"]["authors"].length;

    $.each(jsonData["copyright"]["authors"], function(index, element){
        $('#authors').append(element["name"]);
        if(index < numElements-1){
            $('#authors').append(", ");
        }
    });

    $('#imagetags').empty();
    $.each(jsonData["tags"], function(index, element){
        $('#imagetags').append('<a href="#" class="tag" onclick=\'searchFor("' + element["tag"] + '");\'>' + element["tag"] + '</a>');
    });

    var fullsizeUrl = jsonData["images"]["full"]["url"];

    $('#imageview').empty().append('<a href="' + fullsizeUrl + '" target="_blank"><img src="' + fullsizeUrl + '"/></a>');
}

function searchFor(keyword) {
    $('#tags').val(keyword);
    search();
}

function loadImage(url){
    var request = window.superagent;
    request.get(url).end(
        function(err, res) {
            if(res.ok){
                showImage(res.body);
            } else {
                console.log("Dette gikk ikke bra...");
            }
        }
    )
}

function searchOk(jsonData){
    $('#searchresults').empty();
    $.each(jsonData, function(index, element) {
        var previewImg = '<a href="#" onclick=\'loadImage("' + element["metaUrl"] + '");\'><img src="' + element["previewUrl"] + '"/></a>';
        $('#searchresults').append(previewImg);
    });
}


function search() {
    var tagString = $('#tags').val();
    var minSize = $('#minSize').val();
    var license = $('#withLicense').val();
    var language = $('#inLanguage').val();
    var searchUrl = "/images"

    var request = window.superagent;
    var getRequest = request.get(searchUrl)

    if(tagString) {
        getRequest = getRequest.query("tags=" + tagString)
    }
    if(minSize) {
        getRequest = getRequest.query("minimum-size=" + minSize)
    }
    if(license) {
        getRequest = getRequest.query("license=" + license)
    }
    if(language) {
        getRequest = getRequest.query("language=" + language)
    }

    $('#imagedetail').hide();

    getRequest.end(
        function(err, res) {
            if(res.ok){
                searchOk(res.body);
            } else {
                console.log("Dette gikk ikke bra...");
            }
        }
    )
}