let myMap
let canvas
let mappa
let refreshCb
const options = {
    lat: 59.331,
    lng: 18.03,
    zoom: 14,
    style: "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
}

function initZoneMap(callback) {
    mappa = new Mappa('Leaflet')
    refreshCb = callback
}

function clearZoneMap() {
    clear();
}

function getMapBounds() {
    let b = myMap.map.getBounds()
    return {
        sw: {lat: b.getSouthWest().lat, long: b.getSouthWest().lng},
        ne: {lat: b.getNorthEast().lat, long: b.getNorthEast().lng}
    }
}

function drawZone(zone) {
    const size = max(myMap.zoom() - 4, 2)
    fill(255, 130, 100);
    if (size > 8) {
        stroke(0, 0, 0, 255)
        strokeWeight(1)
    } else if (size > 5) {
        const alpha = (size - 5) * 64
        stroke(0, 0, 0, alpha)
        strokeWeight(1)
    } else {
        noStroke()
    }
    const zPos = myMap.latLngToPixel(zone.lat, zone.long);
    ellipse(zPos.x, zPos.y, size, size);
}

function drawRoutes(zone) {
    let count = 0
    if (zone.routes) {
        const fromPos = myMap.latLngToPixel(zone.lat, zone.long);
        zone.routes.forEach(r => {
                if (r.weight > 0) {
                    const toPos = myMap.latLngToPixel(r.toLat, r.toLong);
                    if (abs(fromPos.x - toPos.x) + abs(fromPos.y - toPos.y) > 10) {
                        const off = delta(fromPos, toPos, 5)
                        const norm = normal(fromPos, toPos, 10)
                        setStyleFromWeight(r.weight)
                        bezier(
                            fromPos.x,
                            fromPos.y,
                            fromPos.x + off.x + norm.x,
                            fromPos.y + off.y + norm.y,
                            toPos.x - off.x + norm.x,
                            toPos.y - off.y + norm.y,
                            toPos.x,
                            toPos.y
                        )
                        count++
                    }
                }
            }
        )
    }
    return count
}

const alphas = [0, 128, 192, 255, 255, 255, 255, 255, 255, 255, 255]
const strokeWeights = [0, 0.3, 0.5, 0.7, 0.9, 1.5, 2, 2.5, 3, 4, 5]

function setStyleFromWeight(w) {
    const alpha = alphas[w]
    const weight = strokeWeights[w] + 0.2
    noFill()
    stroke(0, 0, 0, alpha)
    strokeWeight(weight)
}

function delta(from, to, frac = 1) {
    return {
        x: int((to.x - from.x) / frac),
        y: int((to.y - from.y) / frac)
    }
}

function normal(from, to, frac = 1) {
    return {
        x: int((from.y - to.y) / frac),
        y: int((to.x - from.x) / frac)
    }
}

function setup() {
    canvas = createCanvas(1024, 800);
    myMap = mappa.tileMap(options);
    myMap.overlay(canvas)

    myMap.onChange(reDraw);
}

function draw() {
    // Run on every frame
}

function reDraw() {
    clearZoneMap()
    refreshCb(getMapBounds())
}
