clear
clc

no_frames = 633;
no_video = 10;

images = cell(no_frames,1);
final_images = cell(no_frames,1);
for i = 0: no_frames - 1
    images{i+1} = imread(strcat(string(no_video),'/depth/', string(i), '.png')); 
end

mkdir(strcat(string(no_video),'/depthG'));

for k = 1:no_frames
    img = images{k};
    up = img(:,:,3);
    low = img(:,:,2);
    cnct_up = bitsll(uint16(up), 8);
    cnct_low = uint16(low);
    cnct = bitor(cnct_up, cnct_low);
    final_images{k} = cnct;
    imwrite(cnct,strcat(string(no_video),'/depthG/', string(k-1), '.png'));
end
