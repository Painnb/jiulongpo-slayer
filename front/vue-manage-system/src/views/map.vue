<template>
    <baidu-map 
      class="map" 
      :center="{ lng: 106.552, lat: 29.562 }" 
      :zoom="15"
      @ready="handleMapReady"
    >
      <bm-navigation anchor="BMAP_ANCHOR_TOP_RIGHT"></bm-navigation>
    </baidu-map>
</template>

<script setup>
const handleMapReady = ({ BMap, map }) => {
    console.log("地图已加载", map);
    // 可通过 BMap 调用原生API
    map.setMapStyleV2({     
        styleId: '1a809df884213361b679ae72deded17a'
    });
    map.enableScrollWheelZoom(true);   
};
</script>

<style>
html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;
}

.map {
    position: absolute;
    padding-top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}
</style>


