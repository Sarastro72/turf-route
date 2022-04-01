const WEIGHT_FILTER = 2
const PANEL_HIDE_TIME = 5000
const toRad = Math.PI / 180

let myMap
let zoneLayer
let refreshCb
let llFactor = 1
let zones = []
let selectedZone = null
let panelJob = null

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
        maxZoom: 17
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
    zones = []
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

function drawZone(zone, alpha = 1) {
    let z = L.circle(
        [zone.lat, zone.long],
        zoneOptionFromZoom(alpha)
    )
    let exits = 0
    if (zone.routes) {
        zone.routes.forEach(r => {
            if (r.weight >= WEIGHT_FILTER) exits++
        })
        z.bindTooltip(zone.name)
        z.on('click', function (ev) {
            selectZone(zone)
        });
    }
    z.addTo(zoneLayer)
}

function zoneOptionFromZoom(alpha = 1) {
    let zoom = myMap.getZoom();
    let opacity = 1
    if (zoom === 12) opacity = 0.66
    else if (zoom === 11) opacity = 0.33
    else if (zoom <= 10) opacity = 0.0

    return {
        radius: 30 * Math.pow(1.7, 14 - zoom),
        weight: 1,
        color: "#000000",
        opacity: opacity * alpha,
        fillOpacity: alpha,
        fillColor: "#ff6644"
    }
}

function drawRoutes(zone, alpha = 1, weightLimit = WEIGHT_FILTER, minWeight = 0) {
    let count = 0
    if (zone.routes) {
        zones.push(zone)
        count = drawRoutesInternal(zone, alpha, weightLimit, minWeight)
    }
    return count
}

const targetStyle = {
    color: '#003333',
    fill: false,
    weight: 8,
    opacity: 0
}

function drawRoutesInternal(zone, alpha = 1, weightLimit = WEIGHT_FILTER, minWeight = 0) {
    let count = 0
    if (zone.routes) {
        zone.routes.forEach(r => {
            if (r.weight >= weightLimit) {
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
                let line = L.curve(path, getStyleFromWeight(Math.max(r.weight, minWeight), alpha))
                    .addTo(zoneLayer)
                L.curve(path, targetStyle)
                    .on('mouseover', e => {
                        e.target.setStyle({opacity: 1})
                        infoPanel(zone, r)
                    })
                    .on('mouseout', e => {
                        e.target.setStyle({opacity: 0})
                        hidePanel()
                    })
                    .on('click', e => {
                        console.log(zone)
                        console.log(r)
                        console.log(`${zone.name} to ${r.toName}, times run ${r.timesRun}, median time ${r.medTime}, fastest time ${r.fastestTime} by ${r.fastestUser}`)
                    })
                    .addTo(zoneLayer)
                count++
            }
        })
    }
    return count
}

function selectZone(zone) {
    clearZoneMap()
    if (selectedZone) {
        selectedZone = null
        reDrawAll(1)
    } else {
        selectedZone = zone.id
        reDrawZones(0.75)
        drawRoutes(zone, 1, 0, 3)
        drawZone(zone)
    }
}

function reDrawAll(alpha = 1) {
    zones.forEach(zone => drawRoutesInternal(zone, alpha))
    zones.forEach(zone => drawZone(zone, alpha))
}

function reDrawZones(alpha = 1) {
    zones.forEach(zone => drawZone(zone, alpha))
}

function hidePanel() {
    panelJob = setTimeout(removePanel, PANEL_HIDE_TIME)
}

function removePanel() {
    const old = document.getElementById("infoPanel");
    if (old) document.body.removeChild(old);
}

function infoPanel(z, r) {
    removePanel()
    if (panelJob) {
        clearTimeout(panelJob)
        panelJob = null
    }
    const infoPanel = document.createElement("div");
    infoPanel.id = "infoPanel"
    infoPanel.style.cssText = 'position:absolute;right: 10px;width:20%;height:20%;opacity:1;z-index:100;background:#ffffff;';
    const distance = r.distance - (r.distance % 10)
    infoPanel.innerHTML = `<b>${z.name}</b> to <b>${r.toName}</b><br>` +
        `<b>Distance: </b>${distance}m<br>` +
        `<b>Times recorded: </b>${r.timesRun}<br>` +
        `<b>Median time:</b> ${tm(r.medTime)}<br>` +
        `<b>Average time:</b> ${tm(r.avgTime)}<br>` +
        `<b>Fastest time:</b> ${tm(r.fastestTime)} by <b>${r.fastestUser}</b>`;
    document.body.appendChild(infoPanel);
}

function tm(seconds) {
    if (seconds < 60) {
        return `${seconds}s`
    }
    const min = Math.floor(seconds / 60)
    const sec = seconds % 60
    return `${min}m ${sec}s`
}

const alphas = [0, 0.5, 0.75, 1, 1, 1, 1, 1, 1, 1, 1]
const strokeWeights = [0, 0.3, 0.5, 0.7, 0.9, 1.5, 2, 2.5, 3, 4, 5]

function getStyleFromWeight(w, a = 1) {
    const alpha = alphas[w] * a
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
