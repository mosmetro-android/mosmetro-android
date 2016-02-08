#!/bin/bash

for icon in *.svg; do

    icon=$(echo "$icon" | sed 's|\.svg||g')

    echo "Processing $icon..."

    for dpi in ldpi mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    
        res_path="./res/drawable-$dpi"
        mkdir -p "$res_path"
        
        case "$dpi" in
            ldpi)    size=32  ;;
            mdpi)    size=48  ;;
            hdpi)    size=72  ;;
            xhdpi)   size=96  ;;
            xxhdpi)  size=180 ;;
            xxxhdpi) size=192 ;;
        esac

        echo " $dpi ($size x $size)..."

        inkscape -z -w $size -e $res_path/$icon.png $icon.svg > /dev/null 2>&1

    done

done
