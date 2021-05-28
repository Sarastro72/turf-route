const WEIGHT_FILTER = 2
const toRad = Math.PI / 180

let myMap
let zoneLayer
let refreshCb
let llFactor = 1

const options = {
    lat: 59.331,
    lng: 18.03,
    zoom: 14,
    style: "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
}

function initZoneMap(callback) {
    myMap = L.map('mapId').setView([options.lat, options.lng], options.zoom);
    L.tileLayer(options.style, {
        attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 18
    }).addTo(myMap);
    zoneLayer = L.layerGroup().addTo(myMap);

    refreshCb = callback
    myMap.on('load', function (e) {
        console.log("LOAD")
        refreshCb(getMapBounds())
    });
    myMap.on('moveend', function (e) {
        refreshCb(getMapBounds())
    });
    refreshCb(getMapBounds())
}

function preReDraw() {
    llFactor = Math.cos(myMap.getCenter().lat * toRad)
    clearZoneMap()
}

function clearZoneMap() {
    zoneLayer.clearLayers()
}

function getMapBounds() {
    let b = myMap.getBounds()
    return {
        sw: {lat: b.getSouthWest().lat, long: b.getSouthWest().lng},
        ne: {lat: b.getNorthEast().lat, long: b.getNorthEast().lng}
    }

}

function drawZone(zone) {
    let z = L.circle(
        [zone.lat, zone.long],
        {
            radius: 30,
            weight: 1,
            color: "#000000",
            fillOpacity: 1,
            fillColor: "#ff6644"
        }
    )
    z.bindTooltip(zone.name)
    z.addTo(zoneLayer)
}

function drawRoutes(zone) {
    let count = 0
    if (zone.routes) {
        zone.routes.forEach(r => {
            if (r.weight >= WEIGHT_FILTER) {
                let toPos = {lat: r.toLat, long: r.toLong}
                const off = delta(zone, toPos, 5)
                const norm = normal(zone, toPos, 10)
                let path = [
                    'M',
                    [zone.lat, zone.long],
                    'C',
                    [zone.lat + off.lat + norm.lat, zone.long + off.long + norm.long],
                    [toPos.lat - off.lat + norm.lat, toPos.long - off.long + norm.long],
                    [toPos.lat, toPos.long]]
                L.curve(path, getStyleFromWeight(r.weight))
                    .addTo(zoneLayer)
                count++
            }
        })
    }
    return count
}

const alphas = [0, 128, 192, 255, 255, 255, 255, 255, 255, 255, 255]
const strokeWeights = [0, 0.3, 0.5, 0.7, 0.9, 1.5, 2, 2.5, 3, 4, 5]

function getStyleFromWeight(w) {
    const alpha = alphas[w]
    const weight = strokeWeights[w] + 0.2
    return {
        color: '#000000',
        fill: false,
        weight: weight,
        opacity: alpha
    }
}


function delta(from, to, frac = 1) {
    return {
        lat: (to.lat - from.lat) / frac,
        long: (to.long - from.long) / frac
    }
}

function normal(from, to, frac = 1) {
    return {
        lat: (from.long - to.long) * llFactor / frac,
        long: (to.lat - from.lat) / llFactor / frac
    }
}
